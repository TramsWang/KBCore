package sinc.common;

import sinc.util.DisjointSet;

import java.util.*;

public class Rule {
    private final List<Predicate> rule;
    private final List<sinc.common.Variable> boundedVars;  // Bounded vars use non-negative ids(list index)
    private final List<Integer> boundedVarCnts;
    private RuleFingerPrint fingerPrint;
    private int equivConds;
    private Eval eval;

    public Rule(String headFunctor, int arity) {
        rule = new ArrayList<>();
        boundedVars = new ArrayList<>();
        boundedVarCnts = new ArrayList<>();
        addPred(headFunctor, arity);
        fingerPrint = new RuleFingerPrint(rule);
        equivConds = 0;
        eval = null;
    }

    public Rule(Rule another) {
        this.rule = new ArrayList<>(another.rule.size());
        for (Predicate predicate: another.rule) {
            this.rule.add(new Predicate(predicate));
        }
        this.boundedVars = new ArrayList<>(another.boundedVars);
        this.boundedVarCnts = new ArrayList<>(another.boundedVarCnts);
        this.fingerPrint = another.fingerPrint;
        this.equivConds = another.equivConds;
        this.eval = another.eval;
    }

    public Predicate getPredicate(int idx) {
        return rule.get(idx);
    }

    public Predicate getHead() {
        return rule.get(0);
    }

    public int length() {
        return rule.size();
    }

    public int usedBoundedVars() {
        return boundedVars.size();
    }

    public int size() {
        return equivConds;
    }

    public void setEval(Eval eval) {
        this.eval = eval;
    }

    public Eval getEval() {
        return eval;
    }

    public void addPred(String functor, int arity) {
        Predicate new_pred = new Predicate(functor, arity);
        rule.add(new_pred);
        fingerPrint = new RuleFingerPrint(rule);
    }

    public void boundFreeVar2ExistedVar(int predIdx, int argIdx, int varId) {
        Predicate predicate = rule.get(predIdx);
        if (null == predicate.args[argIdx] && varId < boundedVars.size()) {
            predicate.args[argIdx] = boundedVars.get(varId);
            boundedVarCnts.set(varId, boundedVarCnts.get(varId)+1);
            equivConds++;
            fingerPrint = new RuleFingerPrint(rule);
        }
    }

    public void boundFreeVars2NewVar(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        Predicate predicate1 = rule.get(predIdx1);
        Predicate predicate2 = rule.get(predIdx2);
        if (null == predicate1.args[argIdx1] && null == predicate2.args[argIdx2]) {
            sinc.common.Variable new_var = new sinc.common.Variable(boundedVars.size());
            predicate1.args[argIdx1] = new_var;
            predicate2.args[argIdx2] = new_var;
            boundedVars.add(new_var);
            boundedVarCnts.add(2);
            equivConds++;
            fingerPrint = new RuleFingerPrint(rule);
        }
    }

    public void boundFreeVar2Constant(int predIdx, int argIdx, int constId, String constant) {
        Predicate predicate = rule.get(predIdx);
        if (null == predicate.args[argIdx]) {
            predicate.args[argIdx] = new Constant(constId, constant);
            equivConds++;
            fingerPrint = new RuleFingerPrint(rule);
        }
    }

    public void removeKnownArg(int predIdx, int argIdx) {
        Predicate predicate = rule.get(predIdx);
        Argument argument = predicate.args[argIdx];
        if (null == argument) {
            return;
        }
        predicate.args[argIdx] = null;

        /* 如果删除的是变量，需要调整相关变量的次数和编号 */
        if (argument.isVar) {
            Integer var_uses_cnt = boundedVarCnts.get(argument.id);
            if (2 >= var_uses_cnt) {
                /* 用最后一个var填补删除var的空缺 */
                /* 要注意删除的也可能是最后一个var */
                int last_var_idx = boundedVars.size() - 1;
                Variable last_var = boundedVars.remove(last_var_idx);
                boundedVarCnts.set(argument.id, boundedVarCnts.get(last_var_idx));
                boundedVarCnts.remove(last_var_idx);

                /* 删除本次出现以外，还需要再删除作为自由变量的存在 */
                for (Predicate another_predicate : rule) {
                    for (int i = 0; i < another_predicate.arity(); i++) {
                        if (null != another_predicate.args[i]) {
                            if (argument.id == another_predicate.args[i].id) {
                                another_predicate.args[i] = null;
                            }
                        }
                    }
                }

                if (argument != last_var) {
                    for (Predicate another_predicate : rule) {
                        for (int i = 0; i < another_predicate.arity(); i++) {
                            if (null != another_predicate.args[i]) {
                                if (last_var.id == another_predicate.args[i].id) {
                                    another_predicate.args[i] = argument;
                                }
                            }
                        }
                    }
                }
            } else {
                /* 只删除本次出现 */
                boundedVarCnts.set(argument.id, var_uses_cnt - 1);
            }
        }
        equivConds--;

        /* 删除变量可能出现纯自由的predicate，需要一并删除(head保留) */
        Iterator<Predicate> itr = rule.iterator();
        Predicate head_pred = itr.next();
        while (itr.hasNext()) {
            Predicate body_pred = itr.next();
            boolean is_empty_pred = true;
            for (Argument arg_info: body_pred.args) {
                if (null != arg_info) {
                    is_empty_pred = false;
                    break;
                }
            }
            if (is_empty_pred) {
                itr.remove();
            }
        }

        fingerPrint = new RuleFingerPrint(rule);
    }

    /**
     * 以下几种情况为Invalid：
     *   1. Trivial
     *   2. Independent Fragment
     *
     * @return
     */
    public boolean isInvalid() {
        /* Independent Fragment(可能在找origin的时候出现) */
        /* 用并查集检查 */
        /* Assumption: 没有全部是Free Var或Const的Pred(除了head)，因此把所有Bounded Var根据在一个Pred里出现进行合并即可 */
        DisjointSet disjoint_set = new DisjointSet(boundedVars.size());

        /* Trivial(用Set检查) */
        /* 1. 用Set检查 */
        /* 2. 为了防止进入和Head重复的情况，检查和Head存在相同位置相同参数的情况 */
        Predicate head_pred = rule.get(0);
        {
            /* 先把Head中的变量进行统计加入disjoint set */
            List<Integer> bounded_var_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                Argument argument = head_pred.args[arg_idx];
                if (null != argument && argument.isVar) {
                    bounded_var_ids.add(argument.id);
                }
            }
            if (bounded_var_ids.isEmpty()) {
                if (rule.size() >= 2) {
                    /* Head中没有bounded var但是body不为空，此时head是一个independent fragment */
                    return true;
                }
            } else {
                /* 这里必须判断，因为Head中可能不存在Bounded Var */
                int first_id = bounded_var_ids.get(0);
                for (int i = 1; i < bounded_var_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, bounded_var_ids.get(i));
                }
            }
        }

        Set<Predicate> predicate_set = new HashSet<>();
        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            Predicate body_pred = rule.get(pred_idx);
            if (head_pred.functor.equals(body_pred.functor)) {
                for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                    Argument head_arg = head_pred.args[arg_idx];
                    Argument body_arg = body_pred.args[arg_idx];
                    if (null != head_arg && head_arg.equals(body_arg)) {
                        return true;
                    }
                }
            }

            boolean args_complete = true;
            List<Integer> bounded_var_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                Argument argument = body_pred.args[arg_idx];
                if (null == argument) {
                    args_complete = false;
                } else if (argument.isVar) {
                    bounded_var_ids.add(argument.id);
                }
            }

            if (args_complete) {
                if (!predicate_set.add(body_pred)) {
                    return true;
                }
            }

            /* 在同一个Predicate中出现的Bounded Var合并到一个集合中 */
            if (bounded_var_ids.isEmpty()) {
                /* 如果body的pred中没有bounded var那一定是independent fragment */
                return true;
            } else {
                int first_id = bounded_var_ids.get(0);
                for (int i = 1; i < bounded_var_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, bounded_var_ids.get(i));
                }
            }
        }

        /* 判断是否存在Independent Fragment */
        return 2 <= disjoint_set.totalSets();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("(");
        builder.append(eval).append(')');
        builder.append(rule.get(0).toString()).append(":-");
        if (1 < rule.size()) {
            builder.append(rule.get(1));
            for (int i = 2; i < rule.size(); i++) {
                builder.append(',');
                builder.append(rule.get(i).toString());
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule1 = (Rule) o;
        return this.fingerPrint.equals(rule1.fingerPrint);
    }

    @Override
    public int hashCode() {
        return fingerPrint.hashCode();
    }
}
