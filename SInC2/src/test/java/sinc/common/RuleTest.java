package sinc.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RuleTest {
    static class RuleImpl extends Rule {

        public RuleImpl(String headFunctor, int arity, Set<RuleFingerPrint> cache) {
            super(headFunctor, arity, cache);
        }

        public RuleImpl(RuleImpl another) {
            super(another);
        }

        @Override
        public Rule clone() {
            return new RuleImpl(this);
        }

        @Override
        public void boundFreeVar2ExistingVarHandler(int predIdx, int argIdx, int varId) { }

        @Override
        public void boundFreeVar2ExistingVarHandler(Predicate newPredicate, int argIdx, int varId) { }

        @Override
        public void boundFreeVars2NewVarHandler(int predIdx1, int argIdx1, int predIdx2, int argIdx2) { }

        @Override
        public void boundFreeVars2NewVarHandler(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) { }

        @Override
        public void boundFreeVar2ConstantHandler(int predIdx, int argIdx, String constantSymbol) { }

        @Override
        public void removeBoundedArgHandler(int predIdx, int argIdx) { }

        @Override
        protected Eval calculateEval() {
            return Eval.MIN;
        }
    }

    @Test
    void testConstruction() {
        /* h(X, Y, c) <- p(X), q(?, Y), q(c, X) */
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r = new RuleImpl("h", 3, cache);
        assertTrue(r.toString().contains("h(?,?,?):-"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-"));
        assertEquals(0, r.size());
        assertEquals(1, r.length());
        assertEquals(0, r.usedBoundedVars());
        assertEquals(1, cache.size());

        assertTrue(r.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertTrue(r.toString().contains("h(X0,?,?):-p(X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0)"));
        assertEquals(1, r.size());
        assertEquals(2, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(2, cache.size());

        assertTrue(r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertTrue(r.toString().contains("h(X0,X1,?):-p(X0),q(?,X1)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1)"));
        assertEquals(2, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(3, cache.size());

        assertTrue(r.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertTrue(r.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(X4,X0)"));
        assertEquals(3, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(4, cache.size());

        assertTrue(r.boundFreeVar2Constant(3, 0, "c"));
        assertTrue(r.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(c,X0)"));
        assertEquals(4, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(5, cache.size());

        assertTrue(r.boundFreeVar2Constant(0, 2, "c"));
        assertTrue(r.toString().contains("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-p(X0),q(X2,X1),q(c,X0)"));
        assertEquals(5, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(6, cache.size());

        Predicate predicate_head = new Predicate("h", 3);
        predicate_head.args[0] = new Variable(0);
        predicate_head.args[1] = new Variable(1);
        predicate_head.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        Predicate predicate_body1 = new Predicate("p", 1);
        predicate_body1.args[0] = new Variable(0);
        Predicate predicate_body2 = new Predicate("q", 2);
        predicate_body2.args[1] = new Variable(1);
        Predicate predicate_body3 = new Predicate("q", 2);
        predicate_body3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        predicate_body3.args[1] = new Variable(0);

        assertEquals(predicate_head, r.getPredicate(0));
        assertEquals(predicate_head, r.getHead());
        assertEquals(predicate_body1, r.getPredicate(1));
        assertEquals(predicate_body2, r.getPredicate(2));
        assertEquals(predicate_body3, r.getPredicate(3));
    }

    @Test
    void testConstructionAndRemoval1() {
        /* h(X, Y, c) <- p(X), q(?, Y), q(c, X) */
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r = new RuleImpl("h", 3, cache);
        assertTrue(r.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertTrue(r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertTrue(r.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertTrue(r.boundFreeVar2Constant(3, 0, "c"));
        assertTrue(r.boundFreeVar2Constant(0, 2, "c"));
        assertTrue(r.toString().contains("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-p(X0),q(X2,X1),q(c,X0)"));
        assertEquals(5, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(6, cache.size());

        cache.clear();
        assertTrue(r.removeBoundedArg(1, 0));
        assertTrue(r.toString().contains("h(X0,X1,c):-q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-q(X2,X1),q(c,X0)"));
        assertEquals(4, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(1, cache.size());

        assertTrue(r.removeBoundedArg(2, 0));
        assertTrue(r.toString().contains("h(X0,X1,c):-q(?,X1),q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-q(X2,X1),q(X3,X0)"));
        assertEquals(3, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(2, cache.size());

        assertTrue(r.removeBoundedArg(2, 1));
        assertTrue(r.toString().contains("h(?,X0,c):-q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X1,X0,c):-q(X2,X0)"));
        assertEquals(2, r.size());
        assertEquals(2, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(3, cache.size());

        assertTrue(r.removeBoundedArg(0, 2));
        assertTrue(r.toString().contains("h(?,X0,?):-q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X1,X0,X2):-q(X3,X0)"));
        assertEquals(1, r.size());
        assertEquals(2, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(4, cache.size());

        assertTrue(r.removeBoundedArg(0, 1));
        assertTrue(r.toString().contains("h(?,?,?):-"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-"));
        assertEquals(0, r.size());
        assertEquals(1, r.length());
        assertEquals(0, r.usedBoundedVars());
        assertEquals(5, cache.size());
        assertEquals(new Predicate("h", 3), r.getHead());
    }

    @Test
    void testConstructionAndRemoval2() {
        /* h(X, Y, Z) <- p(X), q(Z, Y), q(Z, X) */
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r = new RuleImpl("h", 3, cache);
        assertTrue(r.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertTrue(r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertTrue(r.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertTrue(r.boundFreeVars2NewVar(2, 0, 0, 2));
        assertTrue(r.boundFreeVar2ExistingVar(3, 0, 2));
        assertTrue(r.toString().contains("h(X0,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertEquals(5, r.size());
        assertEquals(4, r.length());
        assertEquals(3, r.usedBoundedVars());
        assertEquals(6, cache.size());

        cache.clear();
        assertTrue(r.removeBoundedArg(0, 0));
        assertTrue(r.toString().contains("h(?,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X3,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertEquals(4, r.size());
        assertEquals(4, r.length());
        assertEquals(3, r.usedBoundedVars());
        assertEquals(1, cache.size());

        assertTrue(r.removeBoundedArg(3, 1));
        assertTrue(r.toString().contains("h(?,X1,X0):-q(X0,X1),q(X0,?)"));
        assertTrue(r.toCompleteRuleString().contains("h(X2,X1,X0):-q(X0,X1),q(X0,X3)"));
        assertEquals(3, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(2, cache.size());

        assertTrue(r.removeBoundedArg(1, 1));
        assertTrue(r.toString().contains("h(?,?,X0):-q(X0,?),q(X0,?)"));
        assertTrue(r.toCompleteRuleString().contains("h(X1,X2,X0):-q(X0,X3),q(X0,X4)"));
        assertEquals(2, r.size());
        assertEquals(3, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(3, cache.size());

        assertFalse(r.removeBoundedArg(0, 2));
    }

    @Test
    void testCopyConstructor() {
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r1 = new RuleImpl("h", 3, cache);
        assertTrue(r1.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertTrue(r1.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertTrue(r1.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertTrue(r1.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertTrue(r1.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(X4,X0)"));
        assertEquals(3, r1.size());
        assertEquals(4, r1.length());
        assertEquals(2, r1.usedBoundedVars());
        assertEquals(4, cache.size());

        Rule r2 = r1.clone();
        assertTrue(r2.removeBoundedArg(1, 0));
        assertTrue(r2.removeBoundedArg(0, 0));
        assertTrue(r2.toString().contains("h(?,X0,?):-q(?,X0)"));
        assertTrue(r2.toCompleteRuleString().contains("h(X1,X0,X2):-q(X3,X0)"));
        assertEquals(1, r2.size());
        assertEquals(2, r2.length());
        assertEquals(1, r2.usedBoundedVars());
        assertEquals(6, cache.size());

        assertTrue(r1.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertTrue(r1.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(X4,X0)"));
        assertEquals(3, r1.size());
        assertEquals(4, r1.length());
        assertEquals(2, r1.usedBoundedVars());
        assertEquals(6, cache.size());
    }
}
