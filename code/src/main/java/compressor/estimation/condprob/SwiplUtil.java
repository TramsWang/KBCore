package compressor.estimation.condprob;

import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;

import java.util.Map;

public class SwiplUtil {

    public static void appendKnowledge(PrologModule module, Term knowledge) {
        Query q = new Query(
                new Compound(":", new Term[]{
                        new Atom(module.getSessionName()), new Compound("assertz", new Term[]{knowledge})
                })
        );
        q.hasSolution();
        q.close();
    }

    public static void retractKnowledge(PrologModule module, Term knowledge) {
        Query q = new Query(
                new Compound(":", new Term[]{
                        new Atom(module.getSessionName()), new Compound("retract", new Term[]{knowledge})
                })
        );
        q.hasSolution();
        q.close();
    }

    public static Compound substitute(Compound compound, Map<String, Term> binding) {
        Term[] bounded_args = new Term[compound.arity()];
        for (int i = 0; i < bounded_args.length; i++) {
            Term original = compound.arg(i+1);
            bounded_args[i] = binding.getOrDefault(original.name(), original);
        }
        return new Compound(compound.name(), bounded_args);
    }
}
