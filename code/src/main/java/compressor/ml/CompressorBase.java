package compressor.ml;

public abstract class CompressorBase<RuleType> {

    protected final String bkFilePath;
    protected final String hypothesisFilePath;
    protected final String startSetFilePath;
    protected final String counterExampleSetFilePath;
    protected final boolean debug;

    public CompressorBase(
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

    abstract protected RuleType findRule();

    abstract protected void updateKb(RuleType rule);

    abstract protected void writeHypothesis();

    abstract protected void writeStartSet();

    abstract protected void writeCounterExampleSet();

    public final void run() {
        loadBk();

        while (shouldContinue()) {
            RuleType rule = findRule();
            updateKb(rule);
        }

        writeHypothesis();

        writeStartSet();

        writeCounterExampleSet();
    }
}
