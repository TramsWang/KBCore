package sinc.common;

import sinc.util.MultiSet;

import java.util.*;

public class RuleFingerPrint {
    private final String headFunctor;
    private final MultiSet<Set<ArgIndicator>>[] headEquivClasses;
    /* 'otherEquivClasses'可以不必是Multiset，可以用Set代替，因为Extension操作中不会引入Independent Fragment */
    /* 但是为了测试方便和保险起见，这里还是用的Multiset */
    private final MultiSet<MultiSet<Set<ArgIndicator>>> otherEquivClasses;

    public RuleFingerPrint(List<Predicate> rule) {
        /* <Limited Var Id: <Predicate Idx: {Arg Indicator}>> */
        final Map<Integer, Map<Integer, Set<ArgIndicator>>> var_2_equiv_set_map = new HashMap<>();

        /* 先把所有的Equiv Class做出来 */
        int unlimited_var_or_const_id = -1;  //  常量和UV用负数标记id
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.size(); pred_idx++) {
            final Predicate predicate = rule.get(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                final Argument argument = predicate.args[arg_idx];
                if (null == argument) {
                    /* UV 直接创建单独的Set和Multiset */
                    final Map<Integer, Set<ArgIndicator>> var_equiv_class = new HashMap<>();
                    var_equiv_class.put(pred_idx, new HashSet<>(Collections.singleton(
                            new VarIndicator(predicate.functor, arg_idx))
                    ));
                    var_2_equiv_set_map.put(unlimited_var_or_const_id, var_equiv_class);
                    unlimited_var_or_const_id--;
                } else if (argument.isVar) {
                    /* LV 根据之前的分组情况进行汇总 */
                    final Map<Integer, Set<ArgIndicator>> var_equiv_class = var_2_equiv_set_map.computeIfAbsent(
                            argument.id, k -> new HashMap<>()
                    );
                    final Set<ArgIndicator> arg_by_pred = var_equiv_class.computeIfAbsent(
                            pred_idx, k -> new HashSet<>()
                    );
                    arg_by_pred.add(new VarIndicator(predicate.functor, arg_idx));
                } else {
                    /* 常量直接创建单独的Set和Multiset */
                    final Map<Integer, Set<ArgIndicator>> var_equiv_class = new HashMap<>();
                    var_equiv_class.put(pred_idx, new HashSet<>(Arrays.asList(
                            new VarIndicator(predicate.functor, arg_idx), new ConstIndicator(argument.name)
                    )));
                    var_2_equiv_set_map.put(unlimited_var_or_const_id, var_equiv_class);
                    unlimited_var_or_const_id--;
                }
            }
        }

        /* 整理Equiv Class */
        final Map<Integer, MultiSet<Set<ArgIndicator>>> var_2_equiv_class_map = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Set<ArgIndicator>>> entry: var_2_equiv_set_map.entrySet()) {
            final MultiSet<Set<ArgIndicator>> equiv_class = new MultiSet<>();
            for (Set<ArgIndicator> set: entry.getValue().values()) {
                equiv_class.add(set);
            }
            var_2_equiv_class_map.put(entry.getKey(), equiv_class);
        }

        /* 把Head对应的Equiv Class填上 */
        final Predicate head_predicate = rule.get(Rule.HEAD_PRED_IDX);
        final Set<Integer> head_rel_ids = new HashSet<>();
        headFunctor = head_predicate.functor;
        headEquivClasses = new MultiSet[head_predicate.arity()];
        unlimited_var_or_const_id = -1;
        for (int arg_idx = 0; arg_idx < head_predicate.arity(); arg_idx++) {
            final Argument argument = head_predicate.args[arg_idx];
            final int vid;
            if (null !=argument && argument.isVar) {
                vid = argument.id;
            } else {
                vid = unlimited_var_or_const_id;
                unlimited_var_or_const_id--;
            }
            headEquivClasses[arg_idx] = var_2_equiv_class_map.get(vid);
            head_rel_ids.add(vid);
        }

        /* 整理其他的Equiv Class */
        otherEquivClasses = new MultiSet<>();
        for (Map.Entry<Integer, MultiSet<Set<ArgIndicator>>> entry: var_2_equiv_class_map.entrySet()) {
            if (!head_rel_ids.contains(entry.getKey())) {
                otherEquivClasses.add(entry.getValue());
            }
        }
    }

    public String getHeadFunctor() {
        return headFunctor;
    }

    public MultiSet<Set<ArgIndicator>>[] getHeadEquivClasses() {
        return headEquivClasses;
    }

    public MultiSet<MultiSet<Set<ArgIndicator>>> getOtherEquivClasses() {
        return otherEquivClasses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleFingerPrint that = (RuleFingerPrint) o;
        return Objects.equals(headFunctor, that.headFunctor) &&
                Arrays.equals(headEquivClasses, that.headEquivClasses) &&
                Objects.equals(otherEquivClasses, that.otherEquivClasses);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(headFunctor, otherEquivClasses);
        result = 31 * result + Arrays.hashCode(headEquivClasses);
        return result;
    }
}
