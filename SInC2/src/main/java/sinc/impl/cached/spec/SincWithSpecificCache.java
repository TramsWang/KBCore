package sinc.impl.cached.spec;

import sinc.SincConfig;
import sinc.common.*;
import sinc.impl.cached.CachedSinc;

import java.util.Set;

public class SincWithSpecificCache extends CachedSinc {

    public SincWithSpecificCache(SincConfig config, String kbPath, String dumpPath, String logPath) {
        super(
                new SincConfig(
                        config.threads,
                        config.validation,
                        config.debug,
                        config.beamWidth,
                        false,  // Rule Cache 的优化方案不支持向前搜索
                        config.evalMetric,
                        config.minFactCoverage,
                        config.minConstantCoverage,
                        true,
                        -1.0,
                        false,
                        false
                ),
                kbPath,
                dumpPath,
                logPath
        );
    }

    @Override
    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        return new SpecificCachedRule(headFunctor, cache, kb);
    }

    @Override
    protected UpdateResult updateKb(Rule rule) {
        SpecificCachedRule forward_cached_rule = (SpecificCachedRule) rule;
        return forward_cached_rule.updateInKb();
    }
}
