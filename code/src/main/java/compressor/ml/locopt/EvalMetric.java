package compressor.ml.locopt;

public interface EvalMetric {

    double getEvaluation();

    boolean useful();

    void setAsMin();

}
