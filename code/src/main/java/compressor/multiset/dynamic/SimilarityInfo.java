package compressor.multiset.dynamic;

public class SimilarityInfo {
    final double similarity;
    final int predIdx1;
    final int argIdx1;
    final int predIdx2;
    final String pred2;  // 如果是Null则表示被比较的pred2在rule中，用predIdx2
    final int argIdx2;

    public SimilarityInfo(double similarity, int predIdx1, int argIdx1, int predIdx2, String pred2, int argIdx2) {
        this.similarity = similarity;
        this.predIdx1 = predIdx1;
        this.argIdx1 = argIdx1;
        this.predIdx2 = predIdx2;
        this.pred2 = pred2;
        this.argIdx2 = argIdx2;
    }
}
