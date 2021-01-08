package compressor.multiset.dynamic;

public class Validness {
    final int posCnt;
    final int negCnt;

    public Validness(int positiveCnt, int negCnt) {
        this.posCnt = positiveCnt;
        this.negCnt = negCnt;
    }

    public double getValidness() {
        return (double) posCnt / (posCnt + negCnt);
    }
}
