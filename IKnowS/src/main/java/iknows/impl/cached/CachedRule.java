package iknows.impl.cached;

import iknows.common.Rule;
import iknows.common.RuleFingerPrint;

import java.util.Set;

public abstract class CachedRule extends Rule  {
    public final CachedQueryMonitor cacheMonitor = new CachedQueryMonitor();

    public CachedRule(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints) {
        super(headFunctor, arity, searchedFingerprints);
    }

    public CachedRule(Rule another) {
        super(another);
    }
}
