package sinc.impl;

import sinc.common.Predicate;

import java.util.*;

public class MemKB {
    private final Set<Predicate> originalKB = new HashSet<>();
    private final Map<String, Set<Predicate>> functor2Facts = new HashMap<>();
    private final Map<String, Map<String, Set<Predicate>>[]> functor2ArgIdx = new HashMap<>();
    private final Set<String> constants = new HashSet<>();
    private final Set<Predicate> provedFacts = new HashSet<>();

    public void addFact(Predicate predicate) {
        /* 添加到functor索引 */
        originalKB.add(predicate);
        Set<Predicate> predicates = functor2Facts.computeIfAbsent(
                predicate.functor, k -> new HashSet<>()
        );
        predicates.add(predicate);

        /* 添加到argument索引 */
        Map<String, Set<Predicate>>[] arg_indices = functor2ArgIdx.computeIfAbsent(
                predicate.functor, k -> {
                    Map<String, Set<Predicate>>[] _arg_indices = new Map[predicate.arity()];
                    for (int i = 0; i < predicate.arity(); i++) {
                        _arg_indices[i] = new HashMap<>();
                    }
                    return _arg_indices;
                }
        );
        for (int i = 0; i < predicate.arity(); i++) {
            String constant = predicate.args[i].name;
            predicates = arg_indices[i].computeIfAbsent(constant, k -> new HashSet<>());
            predicates.add(predicate);
            constants.add(constant);
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
}
