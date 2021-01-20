package compressor.ml.heap;

public class Validity {
    final double posCnt;
    final double allCnt;
    final double validity;

    public Validity(double posCnt, double allCnt) {
        this.posCnt = posCnt;
        this.allCnt = allCnt;
        this.validity = calculate();
    }

    private double calculate() {
        return posCnt / allCnt;
    }

    @Override
    public String toString() {
        return String.format("Validity=%f; (+)%f; (-)%f", validity, posCnt, allCnt - posCnt);
    }
}
