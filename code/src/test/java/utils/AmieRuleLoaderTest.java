package utils;

import common.JplRule;
import common.Predicate;
import common.Rule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AmieRuleLoaderTest {

    @org.junit.jupiter.api.Test
    void loadRule() {
        final int ID_GENDER = 123;
        final int ID_PARENT = 123908;
        String rule_str = "?f  gender  ?b  ?a  parent  ?f   => ?a  gender  ?b\t0.5\t0.5\t0.5\t40000\t80000\t80000\t?a\t0.0\t0.0\t0.0";
        Map<String, Integer> predicate_map = new HashMap<>();
        Map<String, Integer> constant_map = new HashMap<>();
        predicate_map.put("gender", ID_GENDER);
        predicate_map.put("parent", ID_PARENT);
        Rule rule = null;
        try {
            rule = AmieRuleLoader.loadRule(rule_str, predicate_map, constant_map);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        Predicate[] body = rule.body;
        if (ID_GENDER == body[0].predicate) {
            assertArrayEquals(new int[]{0, 1}, body[0].arguments);
            assertEquals(ID_PARENT, body[1].predicate);
            assertArrayEquals(new int[]{2, 0}, body[1].arguments);
        } else {
            assertEquals(ID_PARENT, body[0].predicate);
            assertArrayEquals(new int[]{2, 0}, body[0].arguments);
            assertEquals(ID_GENDER, body[1].predicate);
            assertArrayEquals(new int[]{0, 1}, body[1].arguments);
        }
        assertArrayEquals(new boolean[]{true, true}, body[0].isVariable);
        assertArrayEquals(new boolean[]{true, true}, body[1].isVariable);
        assertEquals(ID_GENDER, rule.head.predicate);
        assertArrayEquals(new int[]{2, 1}, rule.head.arguments);
        assertArrayEquals(new boolean[]{true, true}, rule.head.isVariable);
    }

    @org.junit.jupiter.api.Test
    void loadRules() {
        String[] rules_str = {
                "?a  parent  ?b   => ?a  father  ?b\t1\t0.5\t0.5\t40000\t80000\t80000\t?b\t0.0\t0.0\t0.0",
                "?e  gender  ?b  ?e  parent  ?a   => ?a  gender  ?b\t0.5\t0.5\t0.5\t40000\t80000\t80000\t?a\t0.0\t0.0\t0.0"
        };
        final int ID_GENDER = 123;
        final int ID_PARENT = 123908;
        final int ID_FATHER = 1202;
        Map<String, Integer> predicate_map = new HashMap<>();
        Map<String, Integer> constant_map = new HashMap<>();
        predicate_map.put("gender", ID_GENDER);
        predicate_map.put("parent", ID_PARENT);
        predicate_map.put("father", ID_FATHER);
        Rule[] rules = AmieRuleLoader.loadRules(rules_str, predicate_map, constant_map);

        Rule rule;
        Predicate[] body;
        Predicate p;
        /* Check Rule #0 */
        rule = rules[0];
        body = rule.body;
        try {
            p = new Predicate(0, ID_PARENT, new int[]{0, 1}, new boolean[]{true, true});
        } catch (Exception e) {
            fail();
            p = null;
        }
        assertEquals(p, body[0]);
        try {
            p = new Predicate(0, ID_FATHER, new int[]{0, 1}, new boolean[]{true, true});
        } catch (Exception e) {
            fail();
            p = null;
        }
        assertEquals(p, rule.head);

        /* Check Rule #1 */
        rule = rules[1];
        List<Predicate> predicates = new ArrayList<>();
        try {
            predicates.add(new Predicate(0, ID_GENDER, new int[]{0, 1}, new boolean[]{true, true}));
            predicates.add(new Predicate(0, ID_PARENT, new int[]{0, 2}, new boolean[]{true, true}));
        } catch (Exception e) {
            fail();
        }
        for (int i = 0; i < predicates.size(); i++) {
            assertEquals(predicates.get(i), body[i]);
        }

        assertEquals(ID_GENDER, rule.head.predicate);
        assertArrayEquals(new int[]{2, 1}, rule.head.arguments);
        assertArrayEquals(new boolean[]{true, true}, rule.head.isVariable);
    }

    @Test
    void toPrologSyntax() {
        String[] rules_str = {
                "?a  parent  ?b   => ?a  father  ?b\t1\t0.5\t0.5\t40000\t80000\t80000\t?b\t0.0\t0.0\t0.0",
                "?e  gender  ?b  ?e  parent  ?a   => ?a  gender  ?b\t0.5\t0.5\t0.5\t40000\t80000\t80000\t?a\t0.0\t0.0\t0.0"
        };
        assertEquals("father(X0,X1):-parent(X0,X1)", AmieRuleLoader.toPrologSyntaxString(rules_str[0]));
        assertEquals("gender(X2,X1):-gender(X0,X1),parent(X0,X2)", AmieRuleLoader.toPrologSyntaxString(rules_str[1]));
    }

    @Test
    void toPrologSyntaxObject() {
        String[] rules_str = {
                "?a  parent  ?b   => ?a  father  ?b\t1\t0.5\t0.5\t40000\t80000\t80000\t?b\t0.0\t0.0\t0.0",
                "?e  gender  ?b  ?e  parent  ?a   => ?a  gender  ?b\t0.5\t0.5\t0.5\t40000\t80000\t80000\t?a\t0.0\t0.0\t0.0"
        };
        JplRule rule = AmieRuleLoader.toPrologSyntaxObject(rules_str[0]);
        assertEquals("father(X0, X1):-parent(X0, X1)", rule.toString());

        rule = AmieRuleLoader.toPrologSyntaxObject(rules_str[1]);
        assertEquals("gender(X2, X1):-gender(X0, X1),parent(X0, X2)", rule.toString());
    }
}