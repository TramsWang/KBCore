package sinc.impl.cached;

import sinc.SInC;
import sinc.SincConfig;
import sinc.common.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SincWithCachedRule extends SInC {

    private final MemKB kb = new MemKB();

    public SincWithCachedRule(SincConfig config, String kbPath, String dumpPath) {
        super(
                new SincConfig(
                        config.threads,
                        config.validation,
                        config.debug,
                        config.beamWidth,
                        false,  // Rule Cache 的优化方案不支持向前搜索
                        config.evalMetric,
                        config.minHeadCoverage,
                        config.minConstantProportion,
                        true,
                        -1.0,
                        false,
                        false
                ),
                kbPath,
                dumpPath
        );
    }

    @Override
    protected KbStatistics loadKb() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(kbPath));
            String line;
            while (null != (line = reader.readLine())) {
                final String[] components = line.split("\t");
                final Predicate predicate = new Predicate(components[0], components.length - 1);
                for (int i = 1; i < components.length; i++) {
                    predicate.args[i - 1] = new Constant(CONST_ID, components[i]);
                }
                kb.addFact(predicate);
            }
            kb.calculatePromisingConstants(config.minConstantProportion);

            return new KbStatistics(kb.totalFacts(), kb.functor2ArityMap.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new KbStatistics(-1, -1);
    }

    @Override
    protected List<String> getTargetFunctors() {
        return kb.getAllFunctors();
    }

    @Override
    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        return new ForwardCachedRule(headFunctor, cache, kb);
    }

    @Override
    protected Map<String, Integer> getFunctor2ArityMap() {
        return kb.getFunctor2ArityMap();
    }

    @Override
    protected Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
        return kb.getFunctor2PromisingConstantMap();
    }

    @Override
    protected UpdateResult updateKb(Rule rule) {
        ForwardCachedRule forward_cached_rule = (ForwardCachedRule) rule;
        return forward_cached_rule.updateInKb();
    }

    @Override
    protected Set<Predicate> getOriginalKb() {
        return kb.getOriginalKB();
    }
}
