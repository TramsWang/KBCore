package sinc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleTest {
    static final int CONST_ID = -1;

    @Test
    void testConstruction() {
        /* h(X, Y, c) <- p(X), q(?, Y), q(c, X) */
        Rule r = new Rule("h", 3);
        assertEquals("(null)h(?,?,?):-", r.toString());
        assertEquals(0, r.size());
        assertEquals(1, r.length());
        assertEquals(0, r.usedBoundedVars());
        assertFalse(r.isInvalid());

        r.addPred("p", 1);
        r.boundFreeVars2NewVar(0, 0, 1, 0);
        assertEquals("(null)h(X0,?,?):-p(X0)", r.toString());
        assertEquals(1, r.size());
        assertEquals(2, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertFalse(r.isInvalid());
        
        r.addPred("q", 2);
        r.boundFreeVars2NewVar(2, 1, 0, 1);
        assertEquals("(null)h(X0,X1,?):-p(X0),q(?,X1)", r.toString());
        assertEquals(2, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertFalse(r.isInvalid());
        
        r.addPred("q", 2);
        r.boundFreeVar2Constant(3, 0, CONST_ID, "c");
        assertEquals("(null)h(X0,X1,?):-p(X0),q(?,X1),q(c,?)", r.toString());
        assertEquals(3, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertTrue(r.isInvalid());
        
        r.boundFreeVar2ExistedVar(3, 1, 0);
        assertEquals("(null)h(X0,X1,?):-p(X0),q(?,X1),q(c,X0)", r.toString());
        assertEquals(4, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertFalse(r.isInvalid());

        r.boundFreeVar2Constant(0, 2, CONST_ID, "c");
        assertEquals("(null)h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)", r.toString());
        assertEquals(5, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertFalse(r.isInvalid());

        Predicate predicate_head = new Predicate("h", 3);
        predicate_head.args[0] = new Variable(0);
        predicate_head.args[1] = new Variable(1);
        predicate_head.args[2] = new Constant(CONST_ID, "c");
        Predicate predicate_body1 = new Predicate("p", 1);
        predicate_body1.args[0] = new Variable(0);
        Predicate predicate_body2 = new Predicate("q", 2);
        predicate_body2.args[1] = new Variable(1);
        Predicate predicate_body3 = new Predicate("q", 2);
        predicate_body3.args[0] = new Constant(CONST_ID, "c");
        predicate_body3.args[1] = new Variable(0);

        assertEquals(predicate_head, r.getPredicate(0));
        assertEquals(predicate_head, r.getHead());
        assertEquals(predicate_body1, r.getPredicate(1));
        assertEquals(predicate_body2, r.getPredicate(2));
        assertEquals(predicate_body3, r.getPredicate(3));
    }
    
    @Test
    void testConstructionAndRemoval() {
        /* Test 1: h(?, X) <- h(?, X) */
        Rule r1 = new Rule("h", 2);
        r1.addPred("h", 2);
        r1.boundFreeVars2NewVar(0, 1, 1, 1);
        assertEquals(1, r1.size());
        assertEquals(2, r1.length());
        assertEquals(1, r1.usedBoundedVars());
        assertTrue(r1.isInvalid());
        assertEquals("(null)h(?,X0):-h(?,X0)", r1.toString());

        r1.removeKnownArg(0, 1);
        assertEquals(0, r1.size());
        assertEquals(1, r1.length());
        assertEquals(0, r1.usedBoundedVars());
        assertFalse(r1.isInvalid());
        assertEquals("(null)h(?,?):-", r1.toString());

        /* Test 2: h(X, ?) <- p(X, ?) */
        Rule r2 = new Rule("h", 2);
        r2.addPred("p", 2);
        r2.boundFreeVars2NewVar(0, 0, 1, 0);
        assertEquals(1, r2.size());
        assertEquals(2, r2.length());
        assertEquals(1, r2.usedBoundedVars());
        assertFalse(r2.isInvalid());
        assertEquals("(null)h(X0,?):-p(X0,?)", r2.toString());

        r2.removeKnownArg(1, 0);
        assertEquals(0, r2.size());
        assertEquals(1, r2.length());
        assertEquals(0, r2.usedBoundedVars());
        assertFalse(r2.isInvalid());
        assertEquals("(null)h(?,?):-", r2.toString());

        /* Test 3: h(X, Y, X) <- p(Y, Z), q(Z, ?) */
        Rule r3 = new Rule("h", 3);
        r3.boundFreeVars2NewVar(0, 0, 0, 2);
        r3.addPred("p", 2);
        r3.boundFreeVars2NewVar(0, 1, 1, 0);
        r3.addPred("q", 2);
        r3.boundFreeVars2NewVar(1, 1, 2, 0);
        assertEquals(3, r3.size());
        assertEquals(3, r3.length());
        assertEquals(3, r3.usedBoundedVars());
        assertFalse(r3.isInvalid());
        assertEquals("(null)h(X0,X1,X0):-p(X1,X2),q(X2,?)", r3.toString());

        r3.removeKnownArg(0, 0);
        assertEquals(2, r3.size());
        assertEquals(3, r3.length());
        assertEquals(2, r3.usedBoundedVars());
        assertFalse(r3.isInvalid());
        assertEquals("(null)h(?,X1,?):-p(X1,X0),q(X0,?)", r3.toString());

        r3.removeKnownArg(0, 1);
        assertEquals(1, r3.size());
        assertEquals(3, r3.length());
        assertEquals(1, r3.usedBoundedVars());
        assertTrue(r3.isInvalid());
        assertEquals("(null)h(?,?,?):-p(?,X0),q(X0,?)", r3.toString());

        r3.removeKnownArg(2, 0);
        assertEquals(0, r3.size());
        assertEquals(1, r3.length());
        assertEquals(0, r3.usedBoundedVars());
        assertFalse(r3.isInvalid());
        assertEquals("(null)h(?,?,?):-", r3.toString());

        /* Test 4: h(X, Y) <- p(X, Z), q(Z, Y) */
        Rule r4 = new Rule("h", 2);
        r4.addPred("p", 2);
        r4.boundFreeVars2NewVar(0, 0, 1, 0);
        r4.addPred("q", 2);
        r4.boundFreeVars2NewVar(0, 1, 2, 1);
        r4.boundFreeVars2NewVar(1, 1, 2, 0);
        assertEquals(3, r4.size());
        assertEquals(3, r4.length());
        assertEquals(3, r4.usedBoundedVars());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(X0,X1):-p(X0,X2),q(X2,X1)", r4.toString());

        r4.removeKnownArg(1, 0);
        assertEquals(2, r4.size());
        assertEquals(3, r4.length());
        assertEquals(2, r4.usedBoundedVars());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(?,X1):-p(?,X0),q(X0,X1)", r4.toString());

        r4.removeKnownArg(2, 0);
        assertEquals(1, r4.size());
        assertEquals(2, r4.length());
        assertEquals(1, r4.size());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(?,X0):-q(?,X0)", r4.toString());

        r4.removeKnownArg(0, 1);
        assertEquals(0, r4.size());
        assertEquals(1, r4.length());
        assertEquals(0, r4.usedBoundedVars());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(?,?):-", r4.toString());

        /* Test 5: h(X, Y):-p(X, Y), q(Y, X) */
        Rule r5 = new Rule("h", 2);
        r5.addPred("p", 2);
        r5.boundFreeVars2NewVar(0, 0, 1, 0);
        r5.boundFreeVars2NewVar(0, 1, 1, 1);
        r5.addPred("q", 2);
        r5.boundFreeVar2ExistedVar(2, 0, 1);
        r5.boundFreeVar2ExistedVar(2, 1, 0);
        assertEquals(4, r5.size());
        assertEquals(3, r5.length());
        assertEquals(2, r5.usedBoundedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,X1):-p(X0,X1),q(X1,X0)", r5.toString());

        r5.removeKnownArg(1, 0);
        assertEquals(3, r5.size());
        assertEquals(3, r5.length());
        assertEquals(2, r5.usedBoundedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,X1):-p(?,X1),q(X1,X0)", r5.toString());

        r5.removeKnownArg(1, 1);
        assertEquals(2, r5.size());
        assertEquals(2, r5.length());
        assertEquals(2, r5.usedBoundedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,X1):-q(X1,X0)", r5.toString());

        r5.removeKnownArg(1, 0);
        assertEquals(1, r5.size());
        assertEquals(2, r5.length());
        assertEquals(1, r5.usedBoundedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,?):-q(?,X0)", r5.toString());

        r5.removeKnownArg(1, 1);
        assertEquals(0, r5.size());
        assertEquals(1, r5.length());
        assertEquals(0, r5.usedBoundedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(?,?):-", r5.toString());

        /* Test 6: h(X, c, Y):-p(X, ?), q(d, Y) */
        Rule r6 = new Rule("h", 3);
        r6.addPred("p", 2);
        r6.boundFreeVars2NewVar(0, 0, 1, 0);
        r6.boundFreeVar2Constant(0, 1, CONST_ID, "c");
        r6.addPred("q", 2);
        r6.boundFreeVars2NewVar(2, 1, 0, 2);
        r6.boundFreeVar2Constant(2, 0, CONST_ID, "d");
        assertEquals(4, r6.size());
        assertEquals(3, r6.length());
        assertEquals(2, r6.usedBoundedVars());
        assertFalse(r6.isInvalid());
        assertEquals("(null)h(X0,c,X1):-p(X0,?),q(d,X1)", r6.toString());

        r6.removeKnownArg(2, 1);
        assertEquals(3, r6.size());
        assertEquals(3, r6.length());
        assertEquals(1, r6.usedBoundedVars());
        assertTrue(r6.isInvalid());
        assertEquals("(null)h(X0,c,?):-p(X0,?),q(d,?)", r6.toString());

        r6.removeKnownArg(2, 0);
        assertEquals(2, r6.size());
        assertEquals(2, r6.length());
        assertEquals(1, r6.usedBoundedVars());
        assertFalse(r6.isInvalid());
        assertEquals("(null)h(X0,c,?):-p(X0,?)", r6.toString());

        r6.removeKnownArg(0, 0);
        assertEquals(1, r6.size());
        assertEquals(1, r6.length());
        assertEquals(0, r6.usedBoundedVars());
        assertFalse(r6.isInvalid());
        assertEquals("(null)h(?,c,?):-", r6.toString());

        r6.removeKnownArg(0, 1);
        assertEquals(0, r6.size());
        assertEquals(1, r6.length());
        assertEquals(0, r6.usedBoundedVars());
        assertFalse(r6.isInvalid());
        assertEquals("(null)h(?,?,?):-", r6.toString());
    }
}