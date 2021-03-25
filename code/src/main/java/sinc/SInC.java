package sinc;

import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;
import sinc.common.EvalMetric;
import sinc.common.GraphNode4Compound;
import sinc.common.Rule;
import sinc.util.PrologModule;
import sinc.util.SwiplUtil;
import sinc.util.graph.GraphView;

import java.util.*;

public abstract class SInC {

    protected final int threadNum;
    protected final int beamWidth;
    protected final EvalMetric evalType;

    protected final String bkFilePath;
    protected final boolean debug;

    public SInC(int threadNum, int beamWidth, EvalMetric evalType, String bkFilePath, boolean debug) {
        this.threadNum = threadNum;
        this.beamWidth = beamWidth;
        this.evalType = evalType;
        this.bkFilePath = bkFilePath;
        this.debug = debug;
    }

    abstract protected int loadBk();

    abstract protected boolean shouldContinue();

    abstract protected Rule findRule();

    abstract protected void updateKb(Rule rule);

    abstract protected void findStartSet();

    abstract protected void findCounterExamples();

    abstract public List<Rule> dumpHypothesis();

    abstract public Set<Compound> dumpStartSet();

    abstract public Set<Compound> dumpCounterExampleSet();

    abstract protected Iterator<Compound> originalBkIterator();

    abstract protected Map<GraphNode4Compound, Set<GraphNode4Compound>> getDependencyGraph();

    public final void run() {
        long time_start = System.currentTimeMillis();
        int original_bk_size = loadBk();
        long time_bk_loaded = System.currentTimeMillis();

        while (shouldContinue()) {
            Rule rule = findRule();
            updateKb(rule);
        }
        long time_hypothesis_found = System.currentTimeMillis();

        findStartSet();
        long time_start_set_found = System.currentTimeMillis();

        findCounterExamples();
        long time_counter_examples_found = System.currentTimeMillis();

        System.out.println(">>> Compression Finished");
        List<Rule> hypothesis = dumpHypothesis();
        Set<Compound> start_set = dumpStartSet();
        Set<Compound> counter_example_set = dumpCounterExampleSet();
        System.out.printf("- Hypothesis(%d rules total):\n", hypothesis.size());
        int hypothesis_size = 0;
        for (int i = 0; i < hypothesis.size(); i++) {
            Rule rule = hypothesis.get(i);
            System.out.printf("%d. %s\n", i, rule);
            hypothesis_size += rule.size();
        }
        System.out.println("- Statistics: ");
        System.out.println("----");
        System.out.printf("# %10s %10s %10s %10s %10s %10s\n", "|B|", "Hypo", "|H|", "|N|", "|A|", "Comp(%)");
        System.out.printf("  %10d %10d %10d %10d %10d %10.2f\n",
                original_bk_size,
                hypothesis.size(),
                hypothesis_size,
                start_set.size(),
                counter_example_set.size(),
                (start_set.size() + counter_example_set.size() + hypothesis_size) * 100.0 / original_bk_size
        );
        System.out.println("----");
        System.out.printf("T(ms) %10s %10s %10s %10s %10s\n", "Load", "Hypo", "N", "A", "Total");
        System.out.printf("      %10d %10d %10d %10d %10d\n",
                time_bk_loaded - time_start,
                time_hypothesis_found - time_bk_loaded,
                time_start_set_found - time_hypothesis_found,
                time_counter_examples_found - time_start_set_found,
                time_counter_examples_found - time_start
        );
        System.out.println("----");
    }

    public boolean validate() {
        long time_start = System.currentTimeMillis();
        List<Rule> hypothesis = dumpHypothesis();
        Set<Compound> start_set = dumpStartSet();
        Set<Compound> counter_examples = dumpCounterExampleSet();
        if (debug) {
            GraphView.draw(originalBkIterator(), getDependencyGraph(), k -> !start_set.contains(k));
        }

        for (Compound fact: start_set) {
            SwiplUtil.appendKnowledge(PrologModule.VALIDATION, fact);
        }
        for (Rule rule: hypothesis) {
            SwiplUtil.appendKnowledge(PrologModule.VALIDATION, Term.textToTerm(rule.toCompleteRuleString()));
        }

        /* Check all facts */
        Set<Compound> uncovered_facts = new HashSet<>();
        Iterator<Compound> original_bk_itr = originalBkIterator();
        while (original_bk_itr.hasNext()) {
            Compound fact = original_bk_itr.next();
            if (start_set.contains(fact)) {
                continue;
            }
            System.out.println("Check Fact: " + fact);
            Query q = new Query(":", new Term[]{
                    new Atom(PrologModule.VALIDATION.getSessionName()), fact
            });
            if (!q.hasSolution()) {
                uncovered_facts.add(fact);
            }
            q.close();
        }
        if (!uncovered_facts.isEmpty()) {
            System.out.printf("%d fact(s) uncovered:\n", uncovered_facts.size());
            for (Compound fact: uncovered_facts) {
                System.out.println(fact);
            }
        }

        /* Check all counter examples */
        Set<Compound> uncovered_counter_examples = new HashSet<>();
        for (Compound counter_example: counter_examples) {
            System.out.println("Check CE: " + counter_example);
            Query q = new Query(":", new Term[]{
                    new Atom(PrologModule.VALIDATION.getSessionName()), counter_example
            });
            if (!q.hasSolution()) {
                uncovered_counter_examples.add(counter_example);
            }
            q.close();
        }
        if (!uncovered_counter_examples.isEmpty()) {
            System.out.printf("%d counter example(s) uncovered:\n", uncovered_counter_examples.size());
            for (Compound counter_example: uncovered_counter_examples) {
                System.out.println(counter_example);
            }
        }
        long time_finished = System.currentTimeMillis();
        System.out.printf("Validation Finished in: %d ms\n", time_finished - time_start);

        return uncovered_facts.isEmpty() && uncovered_counter_examples.isEmpty();
    }
}
