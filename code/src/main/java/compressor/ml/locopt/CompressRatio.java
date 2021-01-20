package compressor.ml.locopt;

public class CompressRatio implements EvalMetric {
    private static final double USEFUL_THRESHOLD = 0.5;

    private double posCnt;
    private double allCnt;
    private int ruleSize;
    private double eval;

    public CompressRatio(double posCnt, double allCnt, int ruleSize) {
        this.posCnt = posCnt;
        this.allCnt = allCnt;
        this.ruleSize = ruleSize;
        this.eval = posCnt / (allCnt + ruleSize);
        this.eval = Double.isNaN(this.eval) ? 0 : this.eval;
    }

    @Override
    public double getEvaluation() {
        return eval;
    }

    @Override
    public boolean useful() {
        return USEFUL_THRESHOLD < eval;
    }

    @Override
    public void setAsMin() {
        posCnt = 0;
        allCnt = 0;
        ruleSize = 0;
        eval = 0;
    }

    @Override
    public String toString() {
        return String.format("Eval=%f; (+)%f; (-)%f; |%d|", eval, posCnt, allCnt - posCnt, ruleSize);
    }
}
