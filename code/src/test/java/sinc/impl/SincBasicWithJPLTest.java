package sinc.impl;

import org.junit.jupiter.api.Test;
import sinc.common.EvalMetric;
import sinc.common.Rule;
import sinc.util.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SincBasicWithJPLTest {
    static final int CONSTANT_ID = -1;

    @Test
    void testTinyHypothesis() {
        /*
         * Hypothesis:
         *      gender(X, male) <- father(X, Y)
         *      gender(X, female) <- mother(X, Y)
         */
        UUID id = UUID.randomUUID();
        final String tmp_bk_file_path = id.toString() + "_bk";
        checkFile(tmp_bk_file_path);

        try {
            FamilyRelationGenerator.generateTiny(tmp_bk_file_path, 10, 0);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        for (EvalMetric eval_type: EvalMetric.values()) {
            SincBasicWithJPL sinc = new SincBasicWithJPL(
                    1,
                    1,
                    eval_type,
                    tmp_bk_file_path,
                    false
            );
            sinc.run();
            assertTrue(sinc.validate());

            try {
                Field hypothesis_field = SincBasicWithJPL.class.getDeclaredField("hypothesis");
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

        deleteFile(tmp_bk_file_path);
    }

    @Test
    void testSimpleHypothesis() {
        /*
         * Hypothesis:
         *      gender(X,male):-father(X,Y).
         *      gender(X,female):-mother(X,Y).
         *      parent(X,Y):-father(X,Y).
         *      parent(X,Y):-mother(X,Y).
         */
        UUID id = UUID.randomUUID();
        final String tmp_bk_file_path = id.toString() + "_bk";
        checkFile(tmp_bk_file_path);

        try {
            FamilyRelationGenerator.generateSimple(tmp_bk_file_path, 10, 0);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        for (EvalMetric eval_type: EvalMetric.values()) {
            SincBasicWithJPL sinc = new SincBasicWithJPL(
                    1,
                    1,
                    eval_type,
                    tmp_bk_file_path,
                    false
            );
            sinc.run();
            assertTrue(sinc.validate());

            try {
                Field hypothesis_field = SincBasicWithJPL.class.getDeclaredField("hypothesis");
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

                Rule r3 = new Rule(FamilyRelationGenerator.FamilyPredicate.PARENT.getName(), 2);
                r3.addPred(FamilyRelationGenerator.FamilyPredicate.FATHER.getName(), 2);
                r3.boundFreeVars2NewVar(0, 0, 1, 0);
                r3.boundFreeVars2NewVar(0, 1, 1, 1);
                assertEquals("(null)parent(X0,X1):-father(X0,X1)", r3.toString());

                Rule r4 = new Rule(FamilyRelationGenerator.FamilyPredicate.PARENT.getName(), 2);
                r4.addPred(FamilyRelationGenerator.FamilyPredicate.MOTHER.getName(), 2);
                r4.boundFreeVars2NewVar(0, 0, 1, 0);
                r4.boundFreeVars2NewVar(0, 1, 1, 1);
                assertEquals("(null)parent(X0,X1):-mother(X0,X1)", r4.toString());

                rule_set.add(r1);
                rule_set.add(r2);
                rule_set.add(r3);
                rule_set.add(r4);

                assertEquals(rule_set, rule_set_sinc);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        deleteFile(tmp_bk_file_path);
    }

    private void checkFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            System.err.println("Temporary File Existed: " + filePath);
            fail();
        }
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        file.deleteOnExit();
    }
}