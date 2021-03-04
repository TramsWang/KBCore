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

    abstract protected void loadBk();

    abstract protected boolean shouldContinue();

    abstract protected Rule findRule();

    abstract protected void updateKb(Rule rule);

    abstract protected void findStartSet();

    abstract protected void findCounterExamples();

    abstract protected List<Rule> dumpHypothesis();

    abstract protected Set<Compound> dumpStartSet();

    abstract protected Set<Compound> dumpCounterExampleSet();

    public final void run() {
        loadBk();

        while (shouldContinue()) {
            Rule rule = findRule();
            updateKb(rule);
        }

        findStartSet();

        findCounterExamples();
    }

    abstract public boolean validate();
}
