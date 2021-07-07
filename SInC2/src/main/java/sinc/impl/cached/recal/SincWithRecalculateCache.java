package sinc.impl.cached.recal;

import sinc.SincConfig;
import sinc.common.Rule;
import sinc.common.RuleFingerPrint;
import sinc.common.UpdateResult;
import sinc.impl.cached.CachedSinc;

import java.util.Set;

public class SincWithRecalculateCache extends CachedSinc {

    public SincWithRecalculateCache(SincConfig config, String kbPath, String dumpPath, String logPath) {
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
        return new RecalculateCachedRule(headFunctor, cache, kb);
    }

    @Override
    protected UpdateResult updateKb(Rule rule) {
        RecalculateCachedRule forward_cached_rule = (RecalculateCachedRule) rule;
        return forward_cached_rule.updateInKb();
    }
}
