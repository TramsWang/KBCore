package sinc.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sinc.SInC;
import sinc.common.EvalMetric;
import sinc.common.Rule;
import sinc.util.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SincFullyOptimizedTest {
    static final String TMP_BK_FILE = "tmp_bk";
    static final String TMP_HYPOTHESIS_FILE = "tmp_hypothesis";
    static final String TMP_START_SET_FILE = "tmp_start_set";
    static final String TMP_COUNTER_EXAMPLE_SET_FILE = "tmp_counter_example_set";
    static final int CONSTANT_ID = -1;

    @Test
    void testTinyHypothesis() {
        /*
         * Hypothesis:
         *      gender(X, male) <- father(X, Y)
         *      gender(X, female) <- mother(X, Y)
         */
        try {
            FamilyRelationGenerator.generateTiny(TMP_BK_FILE, 10, 0);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        for (EvalMetric eval_type: EvalMetric.values()) {
            SincFullyOptimized sinc = new SincFullyOptimized(
                    eval_type,
                    TMP_BK_FILE,
                    TMP_HYPOTHESIS_FILE,
                    TMP_START_SET_FILE,
                    TMP_COUNTER_EXAMPLE_SET_FILE,
                    false
            );
            sinc.run();

            try {
                Field hypothesis_field = SincFullyOptimized.class.getDeclaredField("hypothesis");
                List<Rule> rules_sinc = (List<Rule>) hypothesis_field.get(sinc);
                Set<Rule> rule_set_sinc = new HashSet<>(rules_sinc);
                Set<Rule> rule_set = new HashSet<>();

                Rule r1 = new Rule(FamilyRelationGenerator.OtherPredicate.GENDER.getName(), 2);
                r1.boundFreeVar2Constant(0, 1, CONSTANT_ID, FamilyRelationGenerator.Gender.MALE.getName());
                r1.addPred(FamilyRelationGenerator.FamilyPredicate.FATHER.getName(), 2);
                r1.boundFreeVars2NewVar(0, 0, 1, 0);
                assertEquals("(null)gender(X0,male):-father(X0,?)", r1.toString());

                Rule r2 = new Rule(FamilyRelationGenerator.OtherPredicate.GENDER.getName(), 2);
                r2.boundFreeVar2Constant(0, 1, CONSTANT_ID, FamilyRelationGenerator.Gender.FEMALE.getName());
                r2.addPred(FamilyRelationGenerator.FamilyPredicate.MOTHER.getName(), 2);
                r2.boundFreeVars2NewVar(0, 0, 1, 0);
                assertEquals("(null)gender(X0,female):-mother(X0,?)", r2.toString());

                rule_set.add(r1);
                rule_set.add(r2);

                assertEquals(rule_set, rule_set_sinc);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    @BeforeEach
    private void checkTmpFiles() {
        File bk_file = new File(TMP_BK_FILE);
        File hypothesis_file = new File(TMP_HYPOTHESIS_FILE);
        File start_set_file = new File(TMP_START_SET_FILE);
        File counter_example_file = new File(TMP_COUNTER_EXAMPLE_SET_FILE);

        if (bk_file.exists()) {
            System.err.println("Temporary BK file conflicted!");
            fail();
        }

        if (hypothesis_file.exists()) {
            System.err.println("Temporary Hypothesis file conflicted!");
            fail();
        }

        if (start_set_file.exists()) {
            System.err.println("Temporary Start Set file conflicted!");
            fail();
        }

        if (counter_example_file.exists()) {
            System.err.println("Temporary Counter Example Set file conflicted!");
            fail();
        }
    }

    @AfterEach
    private void cleanTmpFiles() {
        File bk_file = new File(TMP_BK_FILE);
        File hypothesis_file = new File(TMP_HYPOTHESIS_FILE);
        File start_set_file = new File(TMP_START_SET_FILE);
        File counter_example_file = new File(TMP_COUNTER_EXAMPLE_SET_FILE);

        bk_file.deleteOnExit();
        hypothesis_file.deleteOnExit();
        start_set_file.deleteOnExit();
        counter_example_file.deleteOnExit();
    }
}