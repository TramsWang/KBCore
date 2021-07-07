package iknows.impl.cached.spec;

import iknows.IknowsConfig;
import iknows.common.*;
import iknows.impl.cached.CachedIknows;

import java.util.Set;

public class IknowsWithSpecificCache extends CachedIknows {

    public IknowsWithSpecificCache(IknowsConfig config, String kbPath, String dumpPath, String logPath) {
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
        return new SpecificCachedRule(headFunctor, cache, kb);
    }

    @Override
    protected UpdateResult updateKb(Rule rule) {
        SpecificCachedRule forward_cached_rule = (SpecificCachedRule) rule;
        return forward_cached_rule.updateInKb();
    }
}
