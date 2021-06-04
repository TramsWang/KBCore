package sinc.common;

import org.junit.jupiter.api.Test;
import sinc.util.MultiSet;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RuleFingerPrintTest {
    static final String FUNCTOR_FATHER = "father";
    static final String FUNCTOR_PARENT = "parent";
    static final String FUNCTOR_GRANDPARENT = "grandParent";
    static final int ARITY_FATHER = 2;
    static final int ARITY_PARENT = 2;
    static final int ARITY_GRANDPARENT = 2;
    final int CONST_ID = -1;

    @Test
    public void testConstruction() {
        /* h(X, c) <- p(X, Y), q(Y, Z, e), h(Z, ?), h(X, ?) */
        /* Equivalence Classes:
         * - In Head:
         *      X: {h[0], p[0], h[0]}
         *      c: {h[1], c}
         * - In Body:
         *      Y: {p[1], q[0]}
         *      Z: {q[1], h[0]}
         *      e: {q[2], e}
         *      ?: {h[1]}, {h[1]}
         */
        Rule rule = new Rule("h", 2);
        rule.addPred("p", 2);
        rule.boundFreeVars2NewVar(0, 0, 1, 0);
        rule.boundFreeVar2Constant(0, 1, CONST_ID, "c");
        rule.addPred("q", 3);
        rule.boundFreeVars2NewVar(1, 1, 2, 0);
        rule.boundFreeVar2Constant(2, 2, CONST_ID, "e");
        rule.addPred("h", 2);
        rule.boundFreeVars2NewVar(2, 1, 3, 0);
        rule.addPred("h", 2);
        rule.boundFreeVar2ExistedVar(4, 0, 0);

        assertTrue(rule.toString().contains("h(X0,c):-p(X0,X1),q(X1,X2,e),h(X2,?),h(X0,?)"));

        try {
            Field finger_print_field = Rule.class.getDeclaredField("fingerPrint");
            finger_print_field.setAccessible(true);
            RuleFingerPrint finger_print = (RuleFingerPrint) finger_print_field.get(rule);

            Field head_functor_field = RuleFingerPrint.class.getDeclaredField("headFunctor");
            head_functor_field.setAccessible(true);
            String head_functor = (String) head_functor_field.get(finger_print);

            Field head_equiv_classes_field = RuleFingerPrint.class.getDeclaredField("headEquivClasses");
            head_equiv_classes_field.setAccessible(true);
            MultiSet<ArgIndicator>[] head_equiv_classes = (MultiSet<ArgIndicator>[]) head_equiv_classes_field.get(finger_print);

            Field other_equiv_classes_field = RuleFingerPrint.class.getDeclaredField("otherEquivClasses");
            other_equiv_classes_field.setAccessible(true);
            MultiSet<MultiSet<ArgIndicator>> other_equiv_classes = (MultiSet<MultiSet<ArgIndicator>>) other_equiv_classes_field.get(finger_print);

            assertEquals("h", head_functor);

            MultiSet<ArgIndicator> ms_h0 = new MultiSet<>();
            ms_h0.add(new VarIndicator("h", 0));
            ms_h0.add(new VarIndicator("p", 0));
            ms_h0.add(new VarIndicator("h", 0));
            MultiSet<ArgIndicator> ms_h1 = new MultiSet<>();
            ms_h1.add(new VarIndicator("h", 1));
            ms_h1.add(new ConstIndicator("c"));
            assertArrayEquals(new MultiSet[]{ms_h0, ms_h1}, head_equiv_classes);

            MultiSet<ArgIndicator> ms_y = new MultiSet<>();
            ms_y.add(new VarIndicator("p", 1));
            ms_y.add(new VarIndicator("q", 0));
            MultiSet<ArgIndicator> ms_z = new MultiSet<>();
            ms_z.add(new VarIndicator("q", 1));
            ms_z.add(new VarIndicator("h", 0));
            MultiSet<ArgIndicator> ms_e = new MultiSet<>();
            ms_e.add(new VarIndicator("q", 2));
            ms_e.add(new ConstIndicator("e"));
            MultiSet<ArgIndicator> ms_u1 = new MultiSet<>();
            ms_u1.add(new VarIndicator("h", 1));
            MultiSet<ArgIndicator> ms_u2 = new MultiSet<>();
            ms_u2.add(new VarIndicator("h", 1));
            MultiSet<MultiSet<ArgIndicator>> expected_multi_set = new MultiSet<>();
            expected_multi_set.add(ms_y);
            expected_multi_set.add(ms_z);
            expected_multi_set.add(ms_e);
            expected_multi_set.add(ms_u1);
            expected_multi_set.add(ms_u2);
            assertEquals(expected_multi_set, other_equiv_classes);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testEquality() {
        /* R1: h(X, Y) <- h(Y, X) */
        /* R2: h(Y, X) <- h(X, Y) */
        Rule r1 = new Rule("h", 2);
        r1.addPred("h", 2);
        r1.boundFreeVars2NewVar(0, 0, 1, 1);
        r1.boundFreeVars2NewVar(0, 1, 1, 0);
        r1.setEval(Eval.MIN);
        assertTrue(r1.toString().contains("h(X0,X1):-h(X1,X0)"));

        Rule r2 = new Rule("h", 2);
        r2.addPred("h", 2);
        r2.boundFreeVars2NewVar(0, 1, 1, 0);
        r2.boundFreeVars2NewVar(0, 0, 1, 1);
        assertTrue(r2.toString().contains("h(X1,X0):-h(X0,X1)"));

        assertTrue(r1.equals(r2));
        assertTrue(r2.equals(r1));

        /* R3: h(X) <- h(Y) */
        /* R4: h(X) <- h(Y), h(Z) */
        /* 当保证了没有Independent Fragment之后，这种情况实际不会发生 */
        Rule r3 = new Rule("h", 1);
        r3.addPred("h", 1);
        assertEquals("(null)h(?):-h(?)", r3.toString());
        assertTrue(r3.toString().contains("h(?):-h(?)"));

        Rule r4 = new Rule("h", 1);
        r4.addPred("h", 1);
        r4.addPred("h", 1);
        assertTrue(r4.toString().contains("h(?):-h(?),h(?)"));

        assertFalse(r3.equals(r4));
        assertFalse(r4.equals(r3));

        /* R5: h(X) <- p(X, Y) , q(Y, c) */
        /* R6: h(X) <- p(X, Y) , q(c, Y) */
        Rule r5 = new Rule("h", 1);
        r5.addPred("p", 2);
        r5.boundFreeVars2NewVar(0, 0, 1, 0);
        r5.addPred("q", 2);
        r5.boundFreeVars2NewVar(1, 1, 2, 0);
        r5.boundFreeVar2Constant(2, 1, CONST_ID, "c");
        assertTrue(r5.toString().contains("h(X0):-p(X0,X1),q(X1,c)"));

        Rule r6 = new Rule("h", 1);
        r6.addPred("p", 2);
        r6.boundFreeVars2NewVar(0, 0, 1, 0);
        r6.addPred("q", 2);
        r6.boundFreeVars2NewVar(1, 1, 2, 1);
        r6.boundFreeVar2Constant(2, 0, CONST_ID, "c");
        assertTrue(r6.toString().contains("h(X0):-p(X0,X1),q(c,X1)"));

        assertFalse(r5.equals(r6));
        assertFalse(r6.equals(r5));
    }

    @Test
    public void test2() {
        /* #1: grandParent(X, Z) :- parent(X, Y), father(Y, Z) */
        final Predicate p11 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p11.args[0] = new Variable(0);
        p11.args[1] = new Variable(2);
        final Predicate p12 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p12.args[0] = new Variable(0);
        p12.args[1] = new Variable(1);
        final Predicate p13 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p13.args[0] = new Variable(1);
        p13.args[1] = new Variable(2);
        final List<Predicate> rule1 = new ArrayList<>(Arrays.asList(p11, p12, p13));
        final RuleFingerPrint finger_print1 = new RuleFingerPrint(rule1);

        /* #2: grandParent(Y, X) :- father(Z, X), parent(Y, Z) */
        final Predicate p21 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p21.args[0] = new Variable(1);
        p21.args[1] = new Variable(0);
        final Predicate p22 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p22.args[0] = new Variable(2);
        p22.args[1] = new Variable(0);
        final Predicate p23 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p23.args[0] = new Variable(1);
        p23.args[1] = new Variable(2);
        final List<Predicate> rule2 = new ArrayList<>(Arrays.asList(p21, p22, p23));
        final RuleFingerPrint finger_print2 = new RuleFingerPrint(rule2);

        /* #3: grandParent(Y, X) :- father(Z, X), parent(Z, Y) */
        final Predicate p31 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p31.args[0] = new Variable(1);
        p31.args[1] = new Variable(0);
        final Predicate p32 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p32.args[0] = new Variable(2);
        p32.args[1] = new Variable(0);
        final Predicate p33 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p33.args[0] = new Variable(2);
        p33.args[1] = new Variable(1);
        final List<Predicate> rule3 = new ArrayList<>(Arrays.asList(p31, p32, p33));
        final RuleFingerPrint finger_print3 = new RuleFingerPrint(rule3);

        /* #4: grandParent(Y, X) :- father(?, X), parent(Y, ?) */
        final Predicate p41 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p41.args[0] = new Variable(1);
        p41.args[1] = new Variable(0);
        final Predicate p42 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p42.args[0] = null;
        p42.args[1] = new Variable(0);
        final Predicate p43 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p43.args[0] = new Variable(1);
        p43.args[1] = null;
        final List<Predicate> rule4 = new ArrayList<>(Arrays.asList(p41, p42, p43));
        final RuleFingerPrint finger_print4 = new RuleFingerPrint(rule4);

        assertEquals(finger_print1, finger_print2);
        assertNotEquals(finger_print1, finger_print3);
        assertNotEquals(finger_print1, finger_print4);
        assertNotEquals(finger_print3, finger_print4);
    }
}