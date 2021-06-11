package sinc.impl.cached;

import sinc.common.Predicate;
import sinc.util.MultiSet;

import java.util.*;

public class MemKB {
    private final Set<Predicate> originalKB = new HashSet<>();
    private final Map<String, Set<Predicate>> functor2Facts = new HashMap<>();
    protected final Map<String, Integer> functor2ArityMap = new HashMap<>();
    private final Map<String, Map<String, Set<Predicate>>[]> functor2ArgIdx = new HashMap<>();
    private final Set<String> constants = new HashSet<>();
    private final Set<Predicate> provedFacts = new HashSet<>();
    private final Map<String, MultiSet<String>[]> functor2ArgSetsMap = new HashMap<>();
    private final Map<String, List<String>[]> functor2PromisingConstMap = new HashMap<>();

    public void addFact(Predicate predicate) {
        /* 添加到functor索引 */
        originalKB.add(predicate);
        functor2Facts.compute(predicate.functor, (func, set) -> {
            if (null == set) {
                set = new HashSet<>();
                functor2ArityMap.put(func, predicate.arity());
            }
            set.add(predicate);
            return set;
        });

        /* 添加到argument索引 */
        final Map<String, Set<Predicate>>[] arg_indices = functor2ArgIdx.computeIfAbsent(
                predicate.functor, k -> {
                    Map<String, Set<Predicate>>[] _arg_indices = new Map[predicate.arity()];
                    for (int i = 0; i < predicate.arity(); i++) {
                        _arg_indices[i] = new HashMap<>();
                    }
                    return _arg_indices;
                }
        );
        final MultiSet<String>[] arg_sets =  functor2ArgSetsMap.computeIfAbsent(
                predicate.functor, k -> {
                    MultiSet<String>[] _arg_set_list = new MultiSet[predicate.arity()];
                    for (int i = 0; i < _arg_set_list.length; i++) {
                        _arg_set_list[i] = new MultiSet<>();
                    }
                    return _arg_set_list;
                }
        );
        for (int i = 0; i < predicate.arity(); i++) {
            final String constant_symbol = predicate.args[i].name;
            arg_indices[i].compute(constant_symbol, (const_sym, set) -> {
                if (null == set) {
                    set = new HashSet<>();
                }
                set.add(predicate);
                return set;
            });
            arg_sets[i].add(constant_symbol);
            constants.add(constant_symbol);
        }
    }

    public void calculatePromisingConstants(double threshold) {
        for (Map.Entry<String, MultiSet<String>[]> entry: functor2ArgSetsMap.entrySet()) {
            MultiSet<String>[] arg_sets = entry.getValue();
            List<String>[] arg_const_lists = new List[arg_sets.length];
            for (int i = 0; i < arg_sets.length; i++) {
                arg_const_lists[i] = arg_sets[i].elementsAboveProportion(threshold);
            }
            functor2PromisingConstMap.put(entry.getKey(), arg_const_lists);
        }
    }

    public int totalConstants() {
        return constants.size();
    }

    public int totalFacts() {
        return originalKB.size();
    }

    public int getArity(String functor) {
        /* 这里不做错误处理，有问题直接抛异常 */
        return functor2ArgIdx.get(functor).length;
    }

    public Set<Predicate> getAllFacts(String functor) {
        /* 这里不做错误处理，有问题直接抛异常 */
        return functor2Facts.get(functor);
    }

    public Set<String> getValueSet(String functor, int argIdx) {
        return functor2ArgIdx.get(functor)[argIdx].keySet();
    }

    public Map<String, Set<Predicate>> getIndices(String functor, int argIdx) {
        return functor2ArgIdx.get(functor)[argIdx];
    }

    public void proveFact(Predicate fact) {
        provedFacts.add(fact);
    }

    public boolean hasProved(Predicate predicate) {
        return provedFacts.contains(predicate);
    }

    public boolean containsFact(Predicate predicate) {
        return originalKB.contains(predicate);
    }

    public Set<String> allConstants() {
        return constants;
    }

    public Iterator<Predicate> factIterator() {
        return originalKB.iterator();
    }

    public Set<Predicate> getOriginalKB() {
        return originalKB;
    }

    public Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
        return functor2PromisingConstMap;
    }

    public List<String> getAllFunctors() {
        return new ArrayList<>(functor2ArityMap.keySet());
    }

    public Map<String, Integer> getFunctor2ArityMap() {
        return functor2ArityMap;
    }

    public int getTotalConstantSubstitutions() {
        int cnt = 0;
        for (MultiSet<String>[] constant_sets: functor2ArgSetsMap.values()) {
            for (MultiSet<String> constant_set: constant_sets) {
                cnt += constant_set.differentValues();
            }
        }
        return cnt;
    }

    public int getActualConstantSubstitutions() {
        int cnt = 0;
        for (List<String>[] promising_constant_lists: functor2PromisingConstMap.values()) {
            for (List<String> promising_constant_list: promising_constant_lists) {
                cnt += promising_constant_list.size();
            }
        }
        return cnt;
    }
}
