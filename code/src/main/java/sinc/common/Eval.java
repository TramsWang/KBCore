package sinc.common;

import java.util.Objects;

public class Eval {
    private static class EvalMin extends Eval {
        private EvalMin() {
            super(0, Double.POSITIVE_INFINITY, Integer.MAX_VALUE);
        }

        @Override
        public double value(EvalMetric type) {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public boolean useful(EvalMetric type) {
            return false;
        }
    }

    public static final Eval MIN = new EvalMin();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Eval eval = (Eval) o;
        return Double.compare(eval.posCnt, posCnt) == 0 &&
                Double.compare(eval.negCnt, negCnt) == 0 &&
                Double.compare(eval.allCnt, allCnt) == 0 &&
                ruleSize == eval.ruleSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(posCnt, negCnt, allCnt, ruleSize);
    }
}
