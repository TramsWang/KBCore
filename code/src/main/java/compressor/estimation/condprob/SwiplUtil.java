package compressor.estimation.condprob;

import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;

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
}
