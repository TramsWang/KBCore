package iknows.impl.cached.recal;

import iknows.IknowsConfig;
import iknows.common.Rule;
import iknows.common.RuleFingerPrint;
import iknows.common.UpdateResult;
import iknows.impl.cached.CachedIknows;

import java.util.Set;

public class IknowsWithRecalculateCache extends CachedIknows {

    public IknowsWithRecalculateCache(IknowsConfig config, String kbPath, String dumpPath, String logPath) {
        super(
                new IknowsConfig(
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
