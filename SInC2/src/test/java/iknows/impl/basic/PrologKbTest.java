package iknows.impl.basic;

import org.jpl7.*;
import org.junit.jupiter.api.Test;
import iknows.common.Constant;
import iknows.common.Predicate;
import iknows.common.Rule;

import java.lang.Integer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PrologKbTest {
    static final String FUNCTOR_FATHER = "father";
    static final String FUNCTOR_PARENT = "parent";
    static final String FUNCTOR_GRANDPARENT = "grandParent";
    static final int ARITY_FATHER = 2;
    static final int ARITY_PARENT = 2;
    static final int ARITY_GRANDPARENT = 2;
    static final int CONST_ID = -1;

    static PrologKb kbFamily() {
        final PrologKb kb = new PrologKb();

        /* father(X, Y):
         *   f1, s1
         *   f2, s2
         *   f2, d2
         *   f3, s3
         *   f4, d4
         */
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate father4 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father4.args[0] = new Constant(CONST_ID, "f3");
        father4.args[1] = new Constant(CONST_ID, "s3");
        Predicate father5 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father5.args[0] = new Constant(CONST_ID, "f4");
        father5.args[1] = new Constant(CONST_ID, "d4");
        kb.addFact(father1);
        kb.addFact(father2);
        kb.addFact(father3);
        kb.addFact(father4);
        kb.addFact(father5);

        /* parent(X, Y):
         *   f1, s1
         *   f1, d1
         *   f2, s2
         *   f2, d2
         *   m2, d2
         *   g1, f1
         *   g2, f2
         *   g2, m2
         *   g3, f3
         */
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent2.args[0] = new Constant(CONST_ID, "f1");
        parent2.args[1] = new Constant(CONST_ID, "d1");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent5 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent5.args[0] = new Constant(CONST_ID, "m2");
        parent5.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate parent8 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent8.args[0] = new Constant(CONST_ID, "g2");
        parent8.args[1] = new Constant(CONST_ID, "m2");
        Predicate parent9 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent9.args[0] = new Constant(CONST_ID, "g3");
        parent9.args[1] = new Constant(CONST_ID, "f3");
        kb.addFact(parent1);
        kb.addFact(parent2);
        kb.addFact(parent3);
        kb.addFact(parent4);
        kb.addFact(parent5);
        kb.addFact(parent6);
        kb.addFact(parent7);
        kb.addFact(parent8);
        kb.addFact(parent9);

        /* grandParent(X, Y):
         *   g1, s1
         *   g2, d2
         *   g4, s4
         */
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        Predicate grand3 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand3.args[0] = new Constant(CONST_ID, "g4");
        grand3.args[1] = new Constant(CONST_ID, "s4");
        kb.addFact(grand1);
        kb.addFact(grand2);
        kb.addFact(grand3);

        /* Constants(16):
         *   g1, g2, g3, g4
         *   f1, f2, f3, f4
         *   m2
         *   s1, s2, s3, s4
         *   d1, d2, d4
         */

        return kb;
    }

    @Test
    void testAddFacts() {
        /* Check Memory Contents */
        final PrologKb kb = kbFamily();
        assertEquals(16, kb.totalConstants());
        assertEquals(17, kb.totalFacts());
        assertEquals(
                new HashSet<>(Arrays.asList(
                        FUNCTOR_FATHER, FUNCTOR_PARENT, FUNCTOR_GRANDPARENT
                )),
                new HashSet<>(kb.getAllFunctors())
        );
        final Map<String, Integer> expected_functor_2_arity_map = new HashMap<>();
        expected_functor_2_arity_map.put(FUNCTOR_FATHER, ARITY_FATHER);
        expected_functor_2_arity_map.put(FUNCTOR_PARENT, ARITY_PARENT);
        expected_functor_2_arity_map.put(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        assertEquals(expected_functor_2_arity_map, kb.getFunctor2ArityMap());

        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate father4 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father4.args[0] = new Constant(CONST_ID, "f3");
        father4.args[1] = new Constant(CONST_ID, "s3");
        Predicate father5 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father5.args[0] = new Constant(CONST_ID, "f4");
        father5.args[1] = new Constant(CONST_ID, "d4");
        final Set<Predicate> father_facts = new HashSet<>();
        father_facts.add(father1);
        father_facts.add(father2);
        father_facts.add(father3);
        father_facts.add(father4);
        father_facts.add(father5);
        assertEquals(father_facts, kb.getGlobalFactsByFunctor(FUNCTOR_FATHER));
        assertEquals(father_facts, kb.getCurrentFactsByFunctor(FUNCTOR_FATHER));
        assertEquals(2, kb.getArity(FUNCTOR_FATHER));

        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent2.args[0] = new Constant(CONST_ID, "f1");
        parent2.args[1] = new Constant(CONST_ID, "d1");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent5 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent5.args[0] = new Constant(CONST_ID, "m2");
        parent5.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate parent8 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent8.args[0] = new Constant(CONST_ID, "g2");
        parent8.args[1] = new Constant(CONST_ID, "m2");
        Predicate parent9 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent9.args[0] = new Constant(CONST_ID, "g3");
        parent9.args[1] = new Constant(CONST_ID, "f3");
        final Set<Predicate> parent_facts = new HashSet<>();
        parent_facts.add(parent1);
        parent_facts.add(parent2);
        parent_facts.add(parent3);
        parent_facts.add(parent4);
        parent_facts.add(parent5);
        parent_facts.add(parent6);
        parent_facts.add(parent7);
        parent_facts.add(parent8);
        parent_facts.add(parent9);
        assertEquals(parent_facts, kb.getGlobalFactsByFunctor(FUNCTOR_PARENT));
        assertEquals(parent_facts, kb.getCurrentFactsByFunctor(FUNCTOR_PARENT));
        assertEquals(2, kb.getArity(FUNCTOR_PARENT));

        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        Predicate grand3 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand3.args[0] = new Constant(CONST_ID, "g4");
        grand3.args[1] = new Constant(CONST_ID, "s4");
        final Set<Predicate> grand_facts = new HashSet<>();
        grand_facts.add(grand1);
        grand_facts.add(grand2);
        grand_facts.add(grand3);
        assertEquals(grand_facts, kb.getGlobalFactsByFunctor(FUNCTOR_GRANDPARENT));
        assertEquals(grand_facts, kb.getCurrentFactsByFunctor(FUNCTOR_GRANDPARENT));
        assertEquals(2, kb.getArity(FUNCTOR_GRANDPARENT));

        final Set<Predicate> all_facts = new HashSet<>();
        all_facts.addAll(father_facts);
        all_facts.addAll(parent_facts);
        all_facts.addAll(grand_facts);
        assertEquals(all_facts, kb.originalKb);

        /* Check Swipl Contents */
        final String v1_name = "X";
        final String v2_name = "Y";
        final Set<Predicate> jpl_facts = new HashSet<>();
        for (String functor: new String[]{
                FUNCTOR_FATHER, FUNCTOR_PARENT, FUNCTOR_GRANDPARENT
        }) {
            Compound query_compound = new Compound(
                    functor, new Term[]{new Variable(v1_name), new Variable(v2_name)}
            );
            Query q = new Query(query_compound);
            for (Map<String, Term> binding : q) {
                final Compound compound = PrologKb.substitute(query_compound, binding);
                jpl_facts.add(PrologKb.compound2Fact(compound));
            }
            q.close();
        }
        assertEquals(all_facts, jpl_facts);
    }

    @Test
    void testPromisingConstants() {
        final PrologKb kb = kbFamily();
        final double threshold = 0.21;
        kb.calculatePromisingConstants(threshold);
        Map<String, Set<String>[]> expected_promising_constants_map = new HashMap<>();
        expected_promising_constants_map.put(
                FUNCTOR_FATHER, new Set[]{
                        new HashSet<String>(Arrays.asList("f2")),
                        new HashSet<String>()
                }
        );
        expected_promising_constants_map.put(
                FUNCTOR_PARENT, new Set[]{
                        new HashSet<String>(Arrays.asList("f1", "f2", "g2")),
                        new HashSet<String>(Arrays.asList("d2"))
                }
        );
        expected_promising_constants_map.put(
                FUNCTOR_GRANDPARENT, new Set[]{
                        new HashSet<String>(Arrays.asList("g1", "g2", "g4")),
                        new HashSet<String>(Arrays.asList("s1", "d2", "s4"))
                }
        );
        Map<String, List<String>[]> actual_promising_constants_map = kb.getFunctor2PromisingConstantMap();
        assertEquals(expected_promising_constants_map.size(), actual_promising_constants_map.size());
        for (Map.Entry<String, Set<String>[]> entry: expected_promising_constants_map.entrySet()) {
            final String functor = entry.getKey();
            final Set<String>[] expected_sets = entry.getValue();
            final List<String>[] actual_lists = actual_promising_constants_map.get(functor);
            assertNotNull(actual_lists);
            assertEquals(expected_sets[0], new HashSet<>(actual_lists[0]));
            assertEquals(expected_sets[1], new HashSet<>(actual_lists[1]));
        }
    }

    @Test
    void testSubstitution() {
        final String v1 = "X";
        final String v2 = "Y";
        final Compound template = new Compound(
                FUNCTOR_FATHER,
                new Term[]{
                        new Variable(v1),
                        new Atom("c"),
                        new Variable(v2)
                }
        );

        final Map<String, Term> binding1 = new HashMap<>();
        binding1.put(v1, new Atom("x"));
        final Compound sub1 = PrologKb.substitute(template, binding1);
        final Compound expected_sub1 = new Compound(
                FUNCTOR_FATHER,
                new Term[]{
                        new Atom("x"),
                        new Atom("c"),
                        new Variable(v2)
                }
        );
        assertEquals(expected_sub1, sub1);

        final Map<String, Term> binding2 = new HashMap<>();
        binding2.put(v2, new Atom("y"));
        final Compound sub2 = PrologKb.substitute(template, binding2);
        final Compound expected_sub2 = new Compound(
                FUNCTOR_FATHER,
                new Term[]{
                        new Variable(v1),
                        new Atom("c"),
                        new Atom("y"),
                }
        );
        assertEquals(expected_sub2, sub2);

        final Map<String, Term> binding3 = new HashMap<>();
        binding3.put(v1, new Atom("x"));
        binding3.put(v2, new Atom("y"));
        final Compound sub3 = PrologKb.substitute(template, binding3);
        final Compound expected_sub3 = new Compound(
                FUNCTOR_FATHER,
                new Term[]{
                        new Atom("x"),
                        new Atom("c"),
                        new Atom("y")
                }
        );
        assertEquals(expected_sub3, sub3);

        final Map<String, Term> binding4 = new HashMap<>();
        binding4.put(v1, new Atom("x"));
        binding4.put(v2, new Atom("y"));
        binding4.put("Z", new Atom("z"));
        final Compound sub4 = PrologKb.substitute(template, binding4);
        final Compound expected_sub4 = new Compound(
                FUNCTOR_FATHER,
                new Term[]{
                        new Atom("x"),
                        new Atom("c"),
                        new Atom("y")
                }
        );
        assertEquals(expected_sub4, sub4);
    }

    @Test
    void testCompound2Fact() {
        final Compound compound = new Compound(
                FUNCTOR_FATHER,
                new Term[]{
                        new Atom("x"),
                        new Atom("c"),
                        new Atom("y")
                }
        );
        final Predicate expected = new Predicate(FUNCTOR_FATHER, 3);
        expected.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "x");
        expected.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        expected.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "y");
        assertEquals(expected, PrologKb.compound2Fact(compound));
    }

    @Test
    void testRetractKnowledge() {
        String[] functors = new String[]{
                FUNCTOR_FATHER, FUNCTOR_PARENT, FUNCTOR_GRANDPARENT
        };
        for (int i = 0; i < 5; i++) {
            /* Append */
            for (String functor: functors) {
                for (int j = 0; j < 300; j++) {
                    PrologKb.appendKnowledge(new Compound(functor, new Term[]{
                            new Atom("x" + j), new Atom("y" + j)
                    }));
                }
            }

            /* Retract */
            int cnt = 0;
            for (String functor: functors) {
                cnt += PrologKb.retractAllKnowledgeFromJpl(functor);
            }
            System.out.printf("Retracted: %d\n", cnt);
            assertEquals(900, cnt);
        }
    }
}