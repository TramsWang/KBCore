package sinc;

import org.jpl7.Compound;
import sinc.common.EvalMetric;
import sinc.common.Rule;

import java.util.List;
import java.util.Set;

public abstract class SInC {

    protected final EvalMetric evalType;

    protected final String bkFilePath;
    protected final String hypothesisFilePath;
    protected final String startSetFilePath;
    protected final String counterExampleSetFilePath;
    protected final boolean debug;

    public SInC(
            EvalMetric evalType,
            String bkFilePath, String hypothesisFilePath, String startSetFilePath, String counterExampleSetFilePath,
            boolean debug
    ) {
        this.evalType = evalType;
        this.bkFilePath = bkFilePath;
        this.hypothesisFilePath = hypothesisFilePath;
        this.startSetFilePath = startSetFilePath;
        this.counterExampleSetFilePath = counterExampleSetFilePath;
        this.debug = debug;
    }

    abstract protected int loadBk();

    abstract protected boolean shouldContinue();

    abstract protected Rule findRule();

    abstract protected void updateKb(Rule rule);

    abstract protected void findStartSet();

    abstract protected void findCounterExamples();

    abstract protected List<Rule> dumpHypothesis();

    abstract protected Set<Compound> dumpStartSet();

    abstract protected Set<Compound> dumpCounterExampleSet();

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

    abstract public boolean validate();
}
