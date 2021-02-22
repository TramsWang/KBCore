package sinc.common;

import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Rule {
    private final List<Predicate> rule;
    private final List<sinc.common.Variable> boundedVars;  // Bounded vars use non-negative ids(list index)
    private final List<Integer> boundedVarCnts;
    private int freeVarPool;  // Free vars use negative ids
    private RuleFingerPrint fingerPrint;
    private int equivConds;
    private Eval eval;

    public Rule(String headFunctor, int arity) {
        rule = new ArrayList<>();
        boundedVars = new ArrayList<>();
        boundedVarCnts = new ArrayList<>();
        freeVarPool = -1 - arity;
        equivConds = 0;
        eval = null;

        /* Add Head Predicate */
        addPred(headFunctor, arity);
        int[] head_free_var_ids = new int[arity];
        for (int i = 0; i < arity; i++) {
            head_free_var_ids[i] = -1 - i;
        }
        fingerPrint = new RuleFingerPrint(headFunctor, head_free_var_ids);
    }

    public Rule(Rule another) {
        this.rule = new ArrayList<>(another.rule.size());
        for (Predicate predicate: another.rule) {
            this.rule.add(new Predicate(predicate));
        }
        this.boundedVars = new ArrayList<>(another.boundedVars);
        this.boundedVarCnts = new ArrayList<>(another.boundedVarCnts);
        this.freeVarPool = another.freeVarPool;
        this.fingerPrint = new RuleFingerPrint(another.fingerPrint);
        this.equivConds = another.equivConds;
        this.eval = another.eval;
    }

    public void addPred(String functor, int arity) {
        Predicate new_pred = new Predicate(functor, arity);
        rule.add(new_pred);
        int[] ids = new int[arity];
        for (int i = 0; i < arity; i++) {
            ids[i] = freeVarPool - i;
        }
        freeVarPool -= arity;
        fingerPrint.createPredicate(functor, ids);
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

    public void setEmptyArg2KnownVar(int predIdx, int argIdx, int varId) {
        Predicate predicate = rule.get(predIdx);
        if (null == predicate.args[argIdx] && varId < boundedVars.size()) {
            predicate.args[argIdx] = boundedVars.get(varId);
            boundedVarCnts.set(varId, boundedVarCnts.get(varId)+1);
            equivConds++;
        }
    }

    public void setEmptyArgs2NewVar(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        PredInfo pred_info_1 = rule.get(predIdx1);
        PredInfo pred_info_2 = rule.get(predIdx2);
        if (null == pred_info_1.args[argIdx1] && null == pred_info_2.args[argIdx2]) {
            ArgInfo new_var = new ArgInfo(boundedVars.size(), ArgType.VAR);
            pred_info_1.args[argIdx1] = new_var;
            pred_info_2.args[argIdx2] = new_var;
            boundedVars.add(new_var);
            boundedVarCnts.add(2);
            equivConds++;
        }
    }

    public JplRule toJplRule() {
        PredInfo head_pred_info = rule.get(0);
        Term[] args = new Term[head_pred_info.args.length];
        for (int arg_idx = 0; arg_idx < args.length; arg_idx++) {
            args[arg_idx] = (null == head_pred_info.args[arg_idx]) ? new Variable("_") :
                    new Variable(head_pred_info.args[arg_idx].name);
        }
        Compound head_compound = new Compound(head_pred_info.predicate, args);

        Compound[] body_compounds = new Compound[rule.size() - 1];
        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            PredInfo body_pred_info = rule.get(pred_idx);
            args = new Term[body_pred_info.args.length];
            for (int arg_idx = 0; arg_idx < args.length; arg_idx++) {
                args[arg_idx] = (null == body_pred_info.args[arg_idx]) ? new Variable("_") :
                        new Variable(body_pred_info.args[arg_idx].name);
            }
            body_compounds[pred_idx - 1] = new Compound(body_pred_info.predicate, args);
        }

        return new JplRule(head_compound, body_compounds);
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

    public void setEval(EvalMetric eval) {
        this.eval = eval;
    }

    public EvalMetric getEval() {
        return eval;
    }

    public void removeKnownVar(int predIdx, int argIdx) {
        PredInfo pred_info = rule.get(predIdx);
        ArgInfo var = pred_info.args[argIdx];
        if (null == var) {
            return;
        }
        pred_info.args[argIdx] = null;
        Integer var_uses_cnt = boundedVarCnts.get(var.id);

        if (2 >= var_uses_cnt) {
            /* 用最后一个var填补删除var的空缺 */
            /* 要注意删除的也可能是最后一个var */
            int last_var_idx = boundedVars.size() - 1;
            ArgInfo last_var = boundedVars.remove(last_var_idx);
            boundedVarCnts.set(var.id, boundedVarCnts.get(last_var_idx));
            boundedVarCnts.remove(last_var_idx);

            /* 删除本次出现以外，还需要再删除作为自由变量的存在 */
            for (PredInfo another_pred_info: rule) {
                for (int i = 0; i < another_pred_info.arity(); i++) {
                    if (null != another_pred_info.args[i]) {
                        if (var.id == another_pred_info.args[i].id) {
                            another_pred_info.args[i] = null;
                        }
                    }
                }
            }

            if (var != last_var) {
                for (PredInfo another_pred_info : rule) {
                    for (int i = 0; i < another_pred_info.arity(); i++) {
                        if (null != another_pred_info.args[i]) {
                            if (last_var.id == another_pred_info.args[i].id) {
                                another_pred_info.args[i] = var;
                            }
                        }
                    }
                }
            }
        } else {
            /* 只删除本次出现 */
            boundedVarCnts.set(var.id, var_uses_cnt-1);
        }
        equivConds--;

        /* 删除变量可能出现纯自由的predicate，需要一并删除(head保留) */
        Iterator<PredInfo> itr = rule.iterator();
        PredInfo head_pred = itr.next();
        while (itr.hasNext()) {
            PredInfo body_pred = itr.next();
            boolean is_empty_pred = true;
            for (ArgInfo arg_info: body_pred.args) {
                if (null != arg_info) {
                    is_empty_pred = false;
                    break;
                }
            }
            if (is_empty_pred) {
                itr.remove();
            }
        }
    }

    public boolean isInvalid() {
        /* Head没有bounded var，且body不为空 */
        PredInfo head_pred_info = rule.get(0);
        boolean no_bounded_var_in_head = true;
        for (ArgInfo head_arg_info: head_pred_info.args) {
            if (null != head_arg_info) {
                no_bounded_var_in_head = false;
                break;
            }
        }
        if (no_bounded_var_in_head && 2 <= rule.size()) {
            return true;
        }

        /* body中有和Head一样的pred（只要存在同样位置的参数一样，就算） */
        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            PredInfo body_pred_info = rule.get(pred_idx);
            if (head_pred_info.predicate.equals(body_pred_info.predicate)) {
                for (int arg_idx = 0; arg_idx < head_pred_info.arity(); arg_idx++) {
                    ArgInfo head_arg = head_pred_info.args[arg_idx];
                    ArgInfo body_arg = body_pred_info.args[arg_idx];
                    if (null != head_arg && head_arg == body_arg) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
