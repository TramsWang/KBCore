package sinc;

import sinc.common.Eval;

public class SincConfig {
    /* Runtime Config */
    public final int threads;
    public final boolean validation;
    public final boolean debug;

    /* Algorithm Strategy Config */
    public final int beamWidth;
    public final boolean searchOrigins;
    public final Eval.EvalMetric evalMetric;

    /* Optimization Config */
    public final boolean ruleCache;  // 如果开启ruleCache，那么searchOrigins被强制设置为false
    public final double sampling;  // 0.0 <= sampling < 1.0, 其他值表示不采样
    public final boolean estimation;
    public final boolean kbBlocking;

    public SincConfig(
            int threads, boolean validation, boolean debug, int beamWidth, boolean searchOrigins,
            Eval.EvalMetric evalMetric, boolean ruleCache, double sampling, boolean estimation,
            boolean kbBlocking
    ) {
        this.threads = threads;
        this.validation = validation;
        this.debug = debug;
        this.beamWidth = beamWidth;
        this.searchOrigins = searchOrigins;
        this.evalMetric = evalMetric;
        this.ruleCache = ruleCache;
        this.sampling = sampling;
        this.estimation = estimation;
        this.kbBlocking = kbBlocking;
    }
}
