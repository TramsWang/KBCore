package sinc.common;

public class Eval {
    private static final double COMP_RATIO_USEFUL_THRESHOLD = 0.5;
    private static final double COMP_CAPACITY_USEFUL_THRESHOLD = 0.0;

    private final double posCnt;
    private final double negCnt;
    private final double allCnt;
    private final int ruleSize;

    private final double compRatio;
    private final double compCapacity;

    public Eval(double posCnt, double allCnt, int ruleSize) {
        this.posCnt = posCnt;
        this.negCnt = allCnt - posCnt;
        this.allCnt = allCnt;
        this.ruleSize = ruleSize;

        double tmp_ratio = posCnt / (allCnt + ruleSize);
        this.compRatio = Double.isNaN(tmp_ratio) ? 0 : tmp_ratio;

        this.compCapacity = posCnt - negCnt - ruleSize;
    }

    public double value(EvalMetric type) {
        switch (type) {
            case CompressRatio:
                return compRatio;
            case CompressionCapacity:
                return compCapacity;
            default:
                return 0;
        }
    }

    public boolean useful(EvalMetric type) {
        switch (type) {
            case CompressRatio:
                return compRatio > COMP_RATIO_USEFUL_THRESHOLD;
            case CompressionCapacity:
                return compCapacity > COMP_CAPACITY_USEFUL_THRESHOLD;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "(+)%f; (-)%f; |%d|; δ=%f; τ=%f", posCnt, negCnt, ruleSize, compCapacity, compRatio
        );
    }
}
