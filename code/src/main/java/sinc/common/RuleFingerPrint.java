package sinc.common;

import sinc.util.MultiSet;

import java.util.*;

public class RuleFingerPrint {
    private final String headFunctor;
    private final MultiSet<ArgIndicator>[] headEquivClasses;
//    private final Set<MultiSet<ArgIndicator>> otherEquivClasses;
    private final MultiSet<MultiSet<ArgIndicator>> otherEquivClasses;

    public RuleFingerPrint(List<Predicate> rule) {
        Predicate head_predicate = rule.get(0);
        headFunctor = head_predicate.functor;
        headEquivClasses = new MultiSet[head_predicate.arity()];
        otherEquivClasses = new MultiSet<>();
        Map<Integer, MultiSet<ArgIndicator>> bounded_equiv_classes = new HashMap<>();

        /* 先处理Head */
        for (int arg_idx = 0; arg_idx < head_predicate.arity(); arg_idx++) {
            Argument argument = head_predicate.args[arg_idx];
            if (null == argument) {
                /* Free Var */
                headEquivClasses[arg_idx] = new MultiSet<>();
                headEquivClasses[arg_idx].add(new VarIndicator(head_predicate.functor, arg_idx));
            } else {
                if (argument.isVar) {
                    final int tmp_idx = arg_idx;
                    bounded_equiv_classes.compute(argument.id, (k, v) -> {
                        if (null == v) {
                            v = new MultiSet<>();
                        }
                        v.add(new VarIndicator(head_predicate.functor, tmp_idx));
                        headEquivClasses[tmp_idx] = v;
                        return v;
                    });
                } else {
                    /* Constant */
                    headEquivClasses[arg_idx] = new MultiSet<>();
                    headEquivClasses[arg_idx].add(new VarIndicator(head_predicate.functor, arg_idx));
                    headEquivClasses[arg_idx].add(new ConstIndicator(argument.name));
                }
            }
        }

        /* 再处理剩余的Body */
        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            Predicate body_predicate = rule.get(pred_idx);
            for (int arg_idx = 0; arg_idx < body_predicate.arity(); arg_idx++) {
                Argument argument = body_predicate.args[arg_idx];
                if (null == argument) {
                    /* Free Var */
                    MultiSet<ArgIndicator> new_equiv_class = new MultiSet<>();
                    new_equiv_class.add(new VarIndicator(body_predicate.functor, arg_idx));
                    otherEquivClasses.add(new_equiv_class);
                } else {
                    if (argument.isVar) {
                        final int tmp_idx = arg_idx;
                        bounded_equiv_classes.compute(argument.id, (k, v) -> {
                            if (null == v) {
                                v = new MultiSet<>();
                                otherEquivClasses.add(v);
                            }
                            v.add(new VarIndicator(body_predicate.functor, tmp_idx));
                            return v;
                        });
                    } else {
                        /* Constant */
                        MultiSet<ArgIndicator> new_equiv_class = new MultiSet<>();
                        new_equiv_class.add(new VarIndicator(body_predicate.functor, arg_idx));
                        new_equiv_class.add(new ConstIndicator(argument.name));
                        otherEquivClasses.add(new_equiv_class);
                    }
                }
            }
        }
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
