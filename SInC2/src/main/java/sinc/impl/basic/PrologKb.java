package sinc.impl.basic;

import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;
import sinc.common.Argument;
import sinc.common.Constant;
import sinc.common.Predicate;
import sinc.common.Rule;
import sinc.util.MultiSet;

import java.util.*;

public class PrologKb {
    protected final Set<Predicate> originalKb = new HashSet<>();
    protected final Map<String, Set<Predicate>> globalFunctor2FactSetMap = new HashMap<>();
    protected final Map<String, Set<Predicate>> curFunctor2FactSetMap = new HashMap<>();
    protected final Map<String, Integer> functor2ArityMap = new HashMap<>();
    protected final Map<String, MultiSet<String>[]> functor2ArgSetsMap = new HashMap<>();
    protected final Set<String> constants = new HashSet<>();
    protected final Map<String, List<String>[]> functor2PromisingConstMap = new HashMap<>();

    public void addFact(Predicate predicate) {
        /* 添加到functor索引 */
        originalKb.add(predicate);
        globalFunctor2FactSetMap.compute(predicate.functor, (func, set) -> {
            if (null == set) {
                set = new HashSet<>();
                functor2ArityMap.put(func, predicate.arity());
            }
            set.add(predicate);
            return set;
        });
        curFunctor2FactSetMap.compute(predicate.functor, (func, set) -> {
            if (null == set) {
                set = new HashSet<>();
            }
            set.add(predicate);
            return set;
        });

        /* 添加到argument索引 */
        final Term[] compound_args = new Term[predicate.arity()];
        functor2ArgSetsMap.compute(predicate.functor, (func, sets) -> {
            if (null == sets) {
                sets = new MultiSet[predicate.arity()];
                for (int i = 0; i < sets.length; i++) {
                    sets[i] = new MultiSet<>();
                }
            }
            for (int i = 0; i < predicate.arity(); i++) {
                final String constant_symbol = predicate.args[i].name;
                sets[i].add(constant_symbol);
                constants.add(constant_symbol);
                compound_args[i] = new Atom(constant_symbol);
            }
            return sets;
        });

        /* 添加到Swipl */
        final Compound compound = new Compound(predicate.functor, compound_args);
        appendKnowledge(compound);
    }

    public void calculatePromisingConstants(double threshold) {
        /* 计算所有符合阈值的constant */
        for (Map.Entry<String, MultiSet<String>[]> entry: functor2ArgSetsMap.entrySet()) {
            MultiSet<String>[] arg_sets = entry.getValue();
            List<String>[] arg_const_lists = new List[arg_sets.length];
            for (int i = 0; i < arg_sets.length; i++) {
                arg_const_lists[i] = arg_sets[i].elementsAboveProportion(threshold);
            }
            functor2PromisingConstMap.put(entry.getKey(), arg_const_lists);
        }
    }

    public Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
        return functor2PromisingConstMap;
    }

    private static void appendKnowledge(Compound compound) {
        Query q = new Query(
                new Compound("assertz", new Term[]{compound})
        );
        q.hasSolution();
        q.close();
    }

    public static Compound substitute(Compound compound, Map<String, Term> binding) {
        Term[] bounded_args = new Term[compound.arity()];
        for (int i = 0; i < bounded_args.length; i++) {
            Term original = compound.arg(i+1);
            bounded_args[i] = binding.getOrDefault(original.name(), original);
        }
        return new Compound(compound.name(), bounded_args);
    }

    public static Predicate substitute(Predicate predicate, Map<String, Term> binding) {
        final Predicate substitution = new Predicate(predicate);
        for (int i = 0; i < substitution.arity(); i++) {
            final Argument argument = substitution.args[i];
            if (null != argument && argument.isVar) {
                Term t = binding.get(argument.name);
                substitution.args[i] = (null == t) ? argument : new Constant(Rule.CONSTANT_ARG_ID, t.name());
            }
        }
        return substitution;
    }

    public int totalConstants() {
        return constants.size();
    }

    public int totalFacts() {
        return originalKb.size();
    }

    public List<String> getAllFunctors() {
        return new ArrayList<>(functor2ArityMap.keySet());
    }

    public Set<Predicate> getGlobalFactsByFunctor(String functor) {
        return globalFunctor2FactSetMap.get(functor);
    }

    public Set<Predicate> getCurrentFactsByFunctor(String functor) {
        return curFunctor2FactSetMap.get(functor);
    }

    public int getArity(String functor) {
        return functor2ArityMap.get(functor);
    }

    public Map<String, Integer> getFunctor2ArityMap() {
        return functor2ArityMap;
    }

    public static Predicate compound2Fact(Compound compound) {
        final Predicate predicate = new Predicate(compound.name(), compound.arity());
        for (int i = 0; i < compound.arity(); i++) {
            predicate.args[i] = new Constant(Rule.CONSTANT_ARG_ID, compound.arg(i+1).name());
        }
        return predicate;
    }

    public Set<String> allConstants() {
        return constants;
    }

    public boolean containsFact(Predicate predicate) {
        return originalKb.contains(predicate);
    }
}
