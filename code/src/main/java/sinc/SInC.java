package sinc;

import sinc.common.Rule;

public abstract class SInC {

    protected final String bkFilePath;
    protected final String hypothesisFilePath;
    protected final String startSetFilePath;
    protected final String counterExampleSetFilePath;
    protected final boolean debug;

    public SInC(
            String bkFilePath, String hypothesisFilePath, String startSetFilePath, String counterExampleSetFilePath,
            boolean debug
    ) {
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

    abstract protected void dumpHypothesis();

    abstract protected void dumpStartSet();

    abstract protected void dumpCounterExampleSet();

    public final void run() {
        loadBk();

        while (shouldContinue()) {
            Rule rule = findRule();
            updateKb(rule);
        }

        dumpHypothesis();

        dumpStartSet();

        dumpCounterExampleSet();
    }
}
