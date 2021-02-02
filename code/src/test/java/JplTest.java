import org.jpl7.*;

import java.util.HashMap;
import java.util.Map;

public class JplTest {
    public void testParent() {
        JPL.init();
        Atom a = new Atom("a");
        Atom b = new Atom("b");
        Atom c = new Atom("c");
        Atom d = new Atom("d");
        Atom e = new Atom("e");
        Compound p1 = new Compound("parent", new Term[]{a, b});
        Compound p2 = new Compound("parent", new Term[]{b, c});
        Compound p3 = new Compound("parent", new Term[]{c, d});
        Compound p4 = new Compound("parent", new Term[]{d, e});
        Query q1 = new Query(new Compound("assertz", new Term[]{p1}));
        Query q2 = new Query(new Compound("assertz", new Term[]{p2}));
        Query q3 = new Query(new Compound("assertz", new Term[]{p3}));
        Query q4 = new Query(new Compound("assertz", new Term[]{p4}));
        q1.hasSolution();q1.close();
        q2.hasSolution();q2.close();
        q3.hasSolution();q3.close();
        q4.hasSolution();q4.close();
        Query q = new Query(new Compound("parent", new Term[]{new Variable("X"), new Variable("Y")}));
        System.out.println("X\tparent\tY");
        for (Map<String, Term> solution: q) {
            System.out.printf("%s\t\t%s\n", solution.get("X"), solution.get("Y"));
        }
        q.close();
    }

    public void testEqual() {
        Atom a = new Atom("a");
        Atom b = new Atom("b");
        Atom c = new Atom("c");

        Compound p1 = new Compound("parent", new Term[]{a, b});
        Compound p2 = new Compound("parent", new Term[]{a, b});
        Compound p3 = new Compound("parent", new Term[]{b, c});

        System.out.printf("parent(a, b) == parent(a, b): %b\n", p1.equals(p2));
        System.out.printf("parent(a, b) == parent(b, c): %b\n", p1.equals(p3));
    }

    public void testDynamicRule() {
        Atom a = new Atom("a");
        Atom b = new Atom("b");
        Atom c = new Atom("c");
        Compound p1 = new Compound("parent", new Term[]{a, b});
        Compound p2 = new Compound("parent", new Term[]{b, c});
        Query q1 = new Query(new Compound("assertz", new Term[]{p1}));
        Query q2 = new Query(new Compound("assertz", new Term[]{p2}));
        q1.hasSolution();q1.close();
        q2.hasSolution();q2.close();

        Query q3 = new Query(Term.textToTerm("assertz((ancestor(X, Y) :- parent(X, Z), parent(Z, Y)))"));
        q3.hasSolution();
        q3.close();
        Query q4 = new Query(new Compound("ancestor", new Term[]{a, b}));
        Query q5 = new Query(new Compound("ancestor", new Term[]{a, c}));
        System.out.println(q4.hasSolution());q4.close();
        System.out.println(q5.hasSolution());q5.close();
    }

    public void testStringfy() {
        Atom a = new Atom("a");
        Atom b = new Atom("b");
        Variable v = new Variable("X");
        Compound p1 = new Compound("parent", new Term[]{a, b});
        Compound p2 = new Compound("ancestor", new Term[]{v, b});

        System.out.printf("%s\t%s\n", a.toString(), a.name());
        System.out.printf("%s\t%s\n", b.toString(), b.name());
        System.out.printf("%s\t%s\n", v.toString(), v.name());
        System.out.printf("%s\t%s\n", p1.toString(), p1.name());
        System.out.printf("%s\t%s\n", p2.toString(), p2.name());
    }

    public void testMultipleKB() {
        JPL.init();
        Atom a = new Atom("a");
        Atom b = new Atom("b");
        Atom c = new Atom("c");
        String session1 = "s1";
        String session2 = "s2";
        Query q = new Query(new Compound(":", new Term[]{
                new Atom(session1), new Compound("assertz", new Term[]{
                        new Compound("parent", new Term[]{a, b})
                })
        }));
        q.hasSolution();q.close();
        q = new Query(new Compound(":", new Term[]{
                new Atom(session1), new Compound("assertz", new Term[]{
                        new Compound("parent", new Term[]{b, c})
                })
        }));
        q.hasSolution();q.close();
        q = new Query(new Compound(":", new Term[]{
                new Atom(session2), new Compound("assertz", new Term[]{
                        new Compound("parent", new Term[]{a, c})
                })
        }));
        q.hasSolution();q.close();

        q = new Query(new Compound(":", new Term[]{
                new Atom(session1), new Compound("parent", new Term[]{new Variable("X"), new Variable("Y")})
        }));
        System.out.printf("Session: %s\n", session1);
        for (Map<String, Term> bindings: q) {
            System.out.printf("%s\t%s\n", bindings.get("X"), bindings.get("Y"));
        }
        q.close();

        q = new Query(new Compound(":", new Term[]{
                new Atom(session2), new Compound("parent", new Term[]{new Variable("X"), new Variable("Y")})
        }));
        System.out.printf("Session: %s\n", session2);
        for (Map<String, Term> bindings: q) {
            System.out.printf("%s\t%s\n", bindings.get("X"), bindings.get("Y"));
        }
        q.close();
    }

    private void testCreatePredicate() {
        JPL.init();
        Query q = new Query(Term.textToTerm("dynamic s1:parent/2"));
        q.hasSolution();q.close();
        q = new Query(":", new Term[]{
                new Atom("s1"), new Compound("parent", new Term[]{
                        new Atom("a"), new Atom("b")
                })
        });
        System.out.println(q.hasSolution());
    }

    private void testSpecialPredicate() {
        JPL.init();
        Term t = Term.textToTerm("=/=(a, b)");
        System.out.println(t.toString());

        Compound compound = new Compound("\\==", new Term[]{new Atom("c"), new Atom("d")});
        System.out.println(compound.name());
        System.out.println(compound.toString());

//        t = Term.textToTerm(compound.toString());
//        System.out.println(t.toString());

        System.out.println("\\==".length());
        System.out.println("\\==");
    }

    private void testMapEquality() {
        Map<String, String> m1 = new HashMap<>();
        m1.put("a", "A");
        m1.put("b", "B");
        Map<String, String> m2 = new HashMap<>();
        m2.put("a", "A");
        m2.put("b", "B");
        System.out.println(m1.equals(m2));

        Map<String, Term> m3 = new HashMap<>();
        m3.put("a", new Atom("A"));
        m3.put("b", new Atom("B"));
        Map<String, Term> m4 = new HashMap<>();
        m4.put("a", new Atom("A"));
        m4.put("b", new Atom("B"));
        System.out.println(m3.equals(m4));
    }

    private void testExistenceQuery() {
        JPL.init();
        Atom a = new Atom("a");
        Atom b = new Atom("b");
        Atom c = new Atom("c");
        Atom d = new Atom("d");
        Atom e = new Atom("e");
        Compound p1 = new Compound("parent", new Term[]{a, b});
        Compound p2 = new Compound("parent", new Term[]{b, c});
        Compound p3 = new Compound("parent", new Term[]{c, d});
        Compound p4 = new Compound("parent", new Term[]{d, e});
        Query q1 = new Query(new Compound("assertz", new Term[]{p1}));
        Query q2 = new Query(new Compound("assertz", new Term[]{p2}));
        Query q3 = new Query(new Compound("assertz", new Term[]{p3}));
        Query q4 = new Query(new Compound("assertz", new Term[]{p4}));
        q1.hasSolution();q1.close();
        q2.hasSolution();q2.close();
        q3.hasSolution();q3.close();
        q4.hasSolution();q4.close();
        Query q = new Query(new Compound("parent", new Term[]{new Variable("X"), new Variable("Y")}));
        System.out.println("X\tparent\tY");
        System.out.println(q.hasSolution());
        q.close();
    }

    public static void main(String[] args) {
        JplTest test = new JplTest();
        test.testExistenceQuery();
    }
}
