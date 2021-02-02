package compressor.ml.locopt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleInfoTest {

    @Test
    public void testRemoveVars() {
        /* Test 1: h(?, X) <- h(?, X) */
        RuleInfo r1 = new RuleInfo("h", 2);
        r1.addPred("h", 2);
        r1.setEmptyArgs2NewVar(0, 1, 1, 1);
        assertEquals(1, r1.size());
        assertEquals(2, r1.predCnt());
        assertEquals(1, r1.usedVars());
        assertTrue(r1.isInvalid());
        assertEquals("(null)h(?,X0):-h(?,X0)", r1.toString());

        r1.removeKnownVar(0, 1);
        assertEquals(0, r1.size());
        assertEquals(1, r1.predCnt());
        assertEquals(0, r1.usedVars());
        assertFalse(r1.isInvalid());
        assertEquals("(null)h(?,?):-", r1.toString());

        /* Test 2: h(X, ?) <- p(X, ?) */
        RuleInfo r2 = new RuleInfo("h", 2);
        r2.addPred("p", 2);
        r2.setEmptyArgs2NewVar(0, 0, 1, 0);
        assertEquals(1, r2.size());
        assertEquals(2, r2.predCnt());
        assertEquals(1, r2.usedVars());
        assertFalse(r2.isInvalid());
        assertEquals("(null)h(X0,?):-p(X0,?)", r2.toString());

        r2.removeKnownVar(1, 0);
        assertEquals(0, r2.size());
        assertEquals(1, r2.predCnt());
        assertEquals(0, r2.usedVars());
        assertFalse(r2.isInvalid());
        assertEquals("(null)h(?,?):-", r2.toString());

        /* Test 3: h(X, Y, X) <- p(Y, Z), q(Z, ?) */
        RuleInfo r3 = new RuleInfo("h", 3);
        r3.setEmptyArgs2NewVar(0, 0, 0, 2);
        r3.addPred("p", 2);
        r3.setEmptyArgs2NewVar(0, 1, 1, 0);
        r3.addPred("q", 2);
        r3.setEmptyArgs2NewVar(1, 1, 2, 0);
        assertEquals(3, r3.size());
        assertEquals(3, r3.predCnt());
        assertEquals(3, r3.usedVars());
        assertFalse(r3.isInvalid());
        assertEquals("(null)h(X0,X1,X0):-p(X1,X2),q(X2,?)", r3.toString());

        r3.removeKnownVar(0, 0);
        assertEquals(2, r3.size());
        assertEquals(3, r3.predCnt());
        assertEquals(2, r3.usedVars());
        assertFalse(r3.isInvalid());
        assertEquals("(null)h(?,X1,?):-p(X1,X0),q(X0,?)", r3.toString());

        r3.removeKnownVar(0, 1);
        assertEquals(1, r3.size());
        assertEquals(3, r3.predCnt());
        assertEquals(1, r3.usedVars());
        assertTrue(r3.isInvalid());
        assertEquals("(null)h(?,?,?):-p(?,X0),q(X0,?)", r3.toString());

        r3.removeKnownVar(2, 0);
        assertEquals(0, r3.size());
        assertEquals(1, r3.predCnt());
        assertEquals(0, r3.usedVars());
        assertFalse(r3.isInvalid());
        assertEquals("(null)h(?,?,?):-", r3.toString());

        /* Test 4: h(X, Y) <- p(X, Z), q(Z, Y) */
        RuleInfo r4 = new RuleInfo("h", 2);
        r4.addPred("p", 2);
        r4.setEmptyArgs2NewVar(0, 0, 1, 0);
        r4.addPred("q", 2);
        r4.setEmptyArgs2NewVar(0, 1, 2, 1);
        r4.setEmptyArgs2NewVar(1, 1, 2, 0);
        assertEquals(3, r4.size());
        assertEquals(3, r4.predCnt());
        assertEquals(3, r4.usedVars());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(X0,X1):-p(X0,X2),q(X2,X1)", r4.toString());

        r4.removeKnownVar(1, 0);
        assertEquals(2, r4.size());
        assertEquals(3, r4.predCnt());
        assertEquals(2, r4.usedVars());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(?,X1):-p(?,X0),q(X0,X1)", r4.toString());

        r4.removeKnownVar(2, 0);
        assertEquals(1, r4.size());
        assertEquals(2, r4.predCnt());
        assertEquals(1, r4.usedVars());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(?,X0):-q(?,X0)", r4.toString());

        r4.removeKnownVar(0, 1);
        assertEquals(0, r4.size());
        assertEquals(1, r4.predCnt());
        assertEquals(0, r4.usedVars());
        assertFalse(r4.isInvalid());
        assertEquals("(null)h(?,?):-", r4.toString());

        /* Test 5: h(X, Y):-p(X, Y), q(Y, X) */
        RuleInfo r5 = new RuleInfo("h", 2);
        r5.addPred("p", 2);
        r5.setEmptyArgs2NewVar(0, 0, 1, 0);
        r5.setEmptyArgs2NewVar(0, 1, 1, 1);
        r5.addPred("q", 2);
        r5.setEmptyArg2KnownVar(2, 0, 1);
        r5.setEmptyArg2KnownVar(2, 1, 0);
        assertEquals(4, r5.size());
        assertEquals(3, r5.predCnt());
        assertEquals(2, r5.usedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,X1):-p(X0,X1),q(X1,X0)", r5.toString());

        r5.removeKnownVar(1, 0);
        assertEquals(3, r5.size());
        assertEquals(3, r5.predCnt());
        assertEquals(2, r5.usedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,X1):-p(?,X1),q(X1,X0)", r5.toString());

        r5.removeKnownVar(1, 1);
        assertEquals(2, r5.size());
        assertEquals(2, r5.predCnt());
        assertEquals(2, r5.usedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,X1):-q(X1,X0)", r5.toString());

        r5.removeKnownVar(1, 0);
        assertEquals(1, r5.size());
        assertEquals(2, r5.predCnt());
        assertEquals(1, r5.usedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(X0,?):-q(?,X0)", r5.toString());

        r5.removeKnownVar(1, 1);
        assertEquals(0, r5.size());
        assertEquals(1, r5.predCnt());
        assertEquals(0, r5.usedVars());
        assertFalse(r5.isInvalid());
        assertEquals("(null)h(?,?):-", r5.toString());
    }
}