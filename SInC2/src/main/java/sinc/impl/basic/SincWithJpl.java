package sinc.impl.basic;

import sinc.SInC;
import sinc.common.Predicate;
import sinc.common.Rule;
import sinc.common.RuleFingerPrint;
import sinc.common.UpdateResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SincWithJpl extends SInC {
    @Override
    protected int loadKb() {
        /* Todo: Implement Here */
        return 0;
    }

    @Override
    protected List<String> getTargetFunctors() {
        /* Todo: Implement Here */
        return null;
    }

    @Override
    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        /* Todo: Implement Here */
        return null;
    }

    @Override
    protected Map<String, Integer> getFunctor2ArityMap() {
        /* Todo: Implement Here */
        return null;
    }

    @Override
    protected Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
        /* Todo: Implement Here */
        return null;
    }

    @Override
    protected UpdateResult updateKb(Rule rule) {
        /* Todo: Implement Here */
        return null;
    }

    @Override
    protected Set<Predicate> getOriginalKb() {
        /* Todo: Implement Here */
        return null;
    }
}
