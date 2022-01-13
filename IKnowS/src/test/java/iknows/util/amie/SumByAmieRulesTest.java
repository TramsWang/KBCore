package iknows.util.amie;

import iknows.IknowsConfig;
import iknows.common.Eval;
import iknows.common.Predicate;
import iknows.common.Rule;
import iknows.common.Variable;
import iknows.common.Dataset;
import iknows.impl.cached.recal.RecalculateCachedRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SumByAmieRulesTest {
    @Test
    void testLoadFile() {
        SumByAmieRules sum = new SumByAmieRules(
                new IknowsConfig(
                        1, false, false, 0, false,
                        Eval.EvalMetric.CompressionCapacity, 0, 1, true,
                        0, false, false
                ), Dataset.FAMILY_SIMPLE.getPath(), null, null,
                "src/test/java/iknows/util/amie/amie_family_simple.result"
        );
        assertEquals(
                new ArrayList<>(Arrays.asList(
                        "?a  parent  ?b   => ?a  mother  ?b	1	0.5	0.5	40	80	80	-2",
                        "?a  parent  ?b   => ?a  father  ?b	1	0.5	0.5	40	80	80	-2",
                        "?a  mother  ?b   => ?a  parent  ?b	0.5	1	1	40	40	40	-1",
                        "?a  father  ?b   => ?a  parent  ?b	0.5	1	1	40	40	40	-1",
                        "?h  gender  ?b  ?a  mother  ?h   => ?a  gender  ?b	0.25	0.5	0.5	20	40	40	-1",
                        "?a  father  ?h  ?h  gender  ?b   => ?a  gender  ?b	0.25	0.5	0.5	20	40	40	-1",
                        "?g  father  ?a  ?g  gender  ?b   => ?a  gender  ?b	0.25	0.5	0.5	20	40	40	-1",
                        "?g  gender  ?b  ?g  parent  ?a   => ?a  gender  ?b	0.5	0.5	0.5	40	80	80	-1",
                        "?h  gender  ?b  ?a  parent  ?h   => ?a  gender  ?b	0.5	0.5	0.5	40	80	80	-1",
                        "?g  gender  ?b  ?g  mother  ?a   => ?a  gender  ?b	0.25	0.5	0.5	20	40	40	-1"
                )), sum.amieRules
        );
    }

    @Test
    void testConvertStructure1() {
        final String RULE_STRING = "?a  parent  ?b   => ?a  mother  ?b	1	0.5	0.5	40	80	80	-2";
        SumByAmieRules sum = new SumByAmieRules(
                new IknowsConfig(
                        1, false, false, 0, false,
                        Eval.EvalMetric.CompressionCapacity, 0, 1, true,
                        0, false, false
                ), Dataset.FAMILY_SIMPLE.getPath(), null, null,
                "src/test/java/iknows/util/amie/amie_family_simple.result"
        );

        List<Predicate> rule_structure = sum.convertAmieRuleStructure(RULE_STRING);
        Predicate head = new Predicate("mother", 2);
        head.args[0] = new Variable(0);
        head.args[1] = new Variable(1);
        Predicate body1 = new Predicate("parent", 2);
        body1.args[0] = new Variable(0);
        body1.args[1] = new Variable(1);
        assertEquals(
                new ArrayList<>(Arrays.asList(head, body1)), rule_structure
        );
    }

    @Test
    void testConvertStructure2() {
        final String RULE_STRING = "?g  gender  ?b  ?g  mother  ?a   => ?a  gender  ?b	0.25	0.5	0.5	20	40	40	-1";
        SumByAmieRules sum = new SumByAmieRules(
                new IknowsConfig(
                        1, false, false, 0, false,
                        Eval.EvalMetric.CompressionCapacity, 0, 1, true,
                        0, false, false
                ), Dataset.FAMILY_SIMPLE.getPath(), null, null,
                "src/test/java/iknows/util/amie/amie_family_simple.result"
        );

        List<Predicate> rule_structure = sum.convertAmieRuleStructure(RULE_STRING);
        Predicate head = new Predicate("gender", 2);
        head.args[0] = new Variable(2);
        head.args[1] = new Variable(1);
        Predicate body1 = new Predicate("gender", 2);
        body1.args[0] = new Variable(0);
        body1.args[1] = new Variable(1);
        Predicate body2 = new Predicate("mother", 2);
        body2.args[0] = new Variable(0);
        body2.args[1] = new Variable(2);
        assertEquals(
                new ArrayList<>(Arrays.asList(head, body1, body2)), rule_structure
        );
    }

    @Test
    void testConstructRule1() {
        final String RULE_STRING = "?a  parent  ?b   => ?a  mother  ?b	1	0.5	0.5	40	80	80	-2";
        SumByAmieRules sum = new SumByAmieRules(
                new IknowsConfig(
                        1, false, false, 0, false,
                        Eval.EvalMetric.CompressionCapacity, 0, 1, true,
                        0, false, false
                ), Dataset.FAMILY_SIMPLE.getPath(), null, null,
                "src/test/java/iknows/util/amie/amie_family_simple.result"
        );
        sum.loadKb();

        List<Predicate> rule_structure = sum.convertAmieRuleStructure(RULE_STRING);
        RecalculateCachedRule rule = sum.constructRuleByStructure(rule_structure);

        RecalculateCachedRule expected_rule = (RecalculateCachedRule) sum.getStartRule("mother", new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar("parent", 2, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar(0, 1, 1, 1));
        assertTrue(expected_rule.toString().contains("mother(X0,X1):-parent(X0,X1)"));
        assertEquals(expected_rule.size(), rule.size());
        assertEquals(expected_rule.length(), rule.length());
        for (int i = 0; i < expected_rule.length(); i++) {
            assertEquals(expected_rule.getPredicate(i), rule.getPredicate(i));
        }
    }

    @Test
    void testConstructRule2() {
        final String RULE_STRING = "?g  father  ?b  ?g  mother  ?a   => ?a  gender  ?b	0.25	0.5	0.5	20	40	40	-1";
        SumByAmieRules sum = new SumByAmieRules(
                new IknowsConfig(
                        1, false, false, 0, false,
                        Eval.EvalMetric.CompressionCapacity, -1, 1, true,
                        0, false, false
                ), Dataset.FAMILY_SIMPLE.getPath(), null, null,
                "src/test/java/iknows/util/amie/amie_family_simple.result"
        );
        sum.loadKb();

        List<Predicate> rule_structure = sum.convertAmieRuleStructure(RULE_STRING);
        RecalculateCachedRule rule = sum.constructRuleByStructure(rule_structure);

        RecalculateCachedRule expected_rule = (RecalculateCachedRule) sum.getStartRule("gender", new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar("mother", 2, 1, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar("father", 2, 1, 0, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar(1, 0, 2, 0));
        assertTrue(expected_rule.toString().contains("gender(X0,X1):-mother(X2,X0),father(X2,X1)"));
        assertEquals(expected_rule.size(), rule.size());
        assertEquals(expected_rule.length(), rule.length());
        for (int i = 0; i < expected_rule.length(); i++) {
            assertEquals(expected_rule.getPredicate(i), rule.getPredicate(i));
        }
    }
}