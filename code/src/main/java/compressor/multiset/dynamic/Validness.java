package compressor.multiset.dynamic;

public class Validness {
    final int posCnt;
    final int negCnt;

    public Validness(int posCnt, int negCnt) {
        this.posCnt = posCnt;
        this.negCnt = negCnt;
    }

    public double getValidness() {
        return (double) posCnt / (posCnt + negCnt);
    }
}
