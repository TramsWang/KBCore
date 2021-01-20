package compressor.ml.locopt;

import common.JplRule;
import compressor.ml.ArgInfo;
import compressor.ml.ArgType;
import compressor.ml.PredInfo;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleInfo {
    private final List<PredInfo> rule;
    private final List<Integer> usedVars;
    private int appliedConditions;
    private EvalMetric evalMetric;

    public RuleInfo(String headPredicate, int arity) {
        rule = new ArrayList<>();
        addPred(headPredicate, arity);
        usedVars = new ArrayList<>();
        appliedConditions = 0;
        evalMetric = null;
    }

    public RuleInfo(RuleInfo another) {
        this.rule = new ArrayList<>(another.rule.size());
        for (PredInfo pred_info: another.rule) {
            this.rule.add(new PredInfo(pred_info));
        }
        this.usedVars = new ArrayList<>(another.usedVars);
        this.appliedConditions = another.appliedConditions;
        this.evalMetric = another.evalMetric;
    }

    public void addPred(String predicate, int arity) {
        PredInfo new_pred = new PredInfo(predicate, arity);
        rule.add(new_pred);
    }

    public PredInfo getPred(int idx) {
        return rule.get(idx);
    }

    public PredInfo getHead() {
        return rule.get(0);
    }

    public int predCnt() {
        return rule.size();
    }

    public int usedVars() {
        return usedVars.size();
    }

    public int size() {
        return appliedConditions;
    }

    public void setEmptyArg2KnownVar(int predIdx, int argIdx, int varId) {
        PredInfo pred_info = rule.get(predIdx);
        if (null == pred_info.args[argIdx] && varId < usedVars.size()) {
            pred_info.args[argIdx] = new ArgInfo(varId, ArgType.VAR);
            usedVars.set(varId, usedVars.get(varId)+1);
            appliedConditions++;
        }
    }

    public void setEmptyArgs2NewVar(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        PredInfo pred_info_1 = rule.get(predIdx1);
        PredInfo pred_info_2 = rule.get(predIdx2);
        if (null == pred_info_1.args[argIdx1] && null == pred_info_2.args[argIdx2]) {
            ArgInfo new_var = new ArgInfo(usedVars.size(), ArgType.VAR);
            pred_info_1.args[argIdx1] = new_var;
            pred_info_2.args[argIdx2] = new_var;
            usedVars.add(2);
            appliedConditions++;
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
        builder.append(evalMetric).append(')');
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

    public void setEvalMetric(EvalMetric evalMetric) {
        this.evalMetric = evalMetric;
    }

    public EvalMetric getEvalMetric() {
        return evalMetric;
    }

    public void removeKnownVar(int predIdx, int argIdx) {
        PredInfo pred_info = rule.get(predIdx);
        ArgInfo var = pred_info.args[argIdx];
        pred_info.args[argIdx] = null;
        Integer var_uses_cnt = usedVars.get(argIdx);
        if (2 >= var_uses_cnt) {
            /* 删除本次出现以外，还需要再删除作为自由变量的存在 */
            for (PredInfo another_pred_info: rule) {
                for (int i = 0; i < another_pred_info.arity(); i++) {
                    if (argIdx == another_pred_info.args[i].id) {
                        another_pred_info.args[i] = null;
                    }
                }
            }
            var_uses_cnt = 0;
        } else {
            /* 只删除本次出现 */
            var_uses_cnt--;
        }
        usedVars.set(argIdx, var_uses_cnt);

        /* 删除变量可能出现纯自由的predicate，需要一并删除 */
        rule.removeIf(predInfo -> {
            for (ArgInfo arg_info: predInfo.args) {
                if (null != arg_info) {
                    return false;
                }
            }
            return true;
        });
    }

    public boolean isTrivial() {
        /* 当Head的所有参数都Bounded以后，Body中出现和其functor一致且参数集是其子集的predicate，是为Trivial */
        PredInfo head_pred_info = rule.get(0);
        Set<Integer> head_vars = new HashSet<>();
        for (ArgInfo arg_info: head_pred_info.args) {
            if (null == arg_info) {
                return false;
            }
            head_vars.add(arg_info.id);
        }

        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            PredInfo body_pred_info  = rule.get(pred_idx);
            if (head_pred_info.predicate.equals(body_pred_info.predicate)) {
                boolean body_vars_in_head_var_set = true;
                for (ArgInfo body_arg_info: body_pred_info.args) {
                    if (!head_vars.contains(body_arg_info.id)) {
                        body_vars_in_head_var_set = false;
                        break;
                    }
                }
                if (body_vars_in_head_var_set) {
                    return true;
                }
            }
        }
        return false;
    }
}
