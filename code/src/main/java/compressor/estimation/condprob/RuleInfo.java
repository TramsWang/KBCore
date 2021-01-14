package compressor.estimation.condprob;

import common.JplRule;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleInfo {
    private final List<PredInfo> rule;
    private final List<Integer> varCntList;
    private int unknownArgCnt;

    public double score;

    public RuleInfo(String headPredicate, int arity) {
        rule = new ArrayList<>();
        PredInfo head_info = new PredInfo(headPredicate, arity);
        for (int i = 0; i < arity; i++) {
            head_info.args[i] = new ArgInfo(i, ArgType.VAR);
        }
        rule.add(head_info);
        varCntList = new ArrayList<>(arity);
        for (int i = 0; i < arity; i++) {
            varCntList.add(1);
        }
        unknownArgCnt = 0;
        score = 0;
    }

    public RuleInfo(RuleInfo another) {
        this.rule = new ArrayList<>(another.rule.size());
        for (PredInfo pred_info: another.rule) {
            this.rule.add(new PredInfo(pred_info));
        }
        this.varCntList = new ArrayList<>(another.varCntList);
        this.unknownArgCnt = another.unknownArgCnt;
        this.score = another.score;
    }

    public void addNewPred(String predicate, int arity) {
        PredInfo new_pred = new PredInfo(predicate, arity);
        rule.add(new_pred);
        unknownArgCnt += arity;
    }

    public int ruleSize() {
        return rule.size();
    }

    public PredInfo getPred(int idx) {
        return rule.get(idx);
    }

    public PredInfo getHead() {
        return rule.get(0);
    }

    public void setUnknownArg(int predIdx, int argIdx, int varId) {
        PredInfo pred_info = rule.get(predIdx);
        if (null == pred_info.args[argIdx]) {
            unknownArgCnt--;
            if (varId < varCntList.size()) {
                pred_info.args[argIdx] = new ArgInfo(varId, ArgType.VAR);
                varCntList.set(varId, varCntList.get(varId) + 1);
            } else {
                pred_info.args[argIdx] = new ArgInfo(varCntList.size(), ArgType.VAR);
                varCntList.add(1);
            }
        }
    }

    public int getVarCnt() {
        return varCntList.size();
    }

    public int getUnknownArgCnt() {
        return unknownArgCnt;
    }

    public JplRule toJplRule() {
        /* 如果仍有变量没有设定，则构造失败 */
        if (0 < unknownArgCnt) {
            return null;
        }
        /* 如果仍然有自由变量，则构造失败 */
        for (Integer cnt: varCntList) {
            if (2 > cnt) {
                return null;
            }
        }

        PredInfo head_pred_info = rule.get(0);
        Term[] args = new Term[head_pred_info.args.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = new Variable(head_pred_info.args[i].name);
        }
        Compound head_compound = new Compound(head_pred_info.predicate, args);

        Compound[] body_compounds = new Compound[rule.size() - 1];
        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            PredInfo body_pred_info = rule.get(pred_idx);
            args = new Term[body_pred_info.args.length];
            for (int arg_idx = 0; arg_idx < args.length; arg_idx++) {
                args[arg_idx] = new Variable(body_pred_info.args[arg_idx].name);
            }
            body_compounds[pred_idx - 1] = new Compound(body_pred_info.predicate, args);
        }

        return new JplRule(head_compound, body_compounds);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(rule.get(0).toString());
        builder.append(":-");
        if (1 < rule.size()) {
            builder.append(rule.get(1));
            for (int i = 2; i < rule.size(); i++) {
                builder.append(',');
                builder.append(rule.get(i).toString());
            }
        }
        return builder.toString();
    }

    public Set<String> NonFreeVarSetInHead() {
        PredInfo head = rule.get(0);
        Set<String> result = new HashSet<>();
        for (ArgInfo arg_info: head.args) {
            if (varCntList.get(arg_info.id) > 1) {
                result.add(arg_info.name);
            }
        }
        return result;
    }

    public boolean isNonFreeVar(int predIdx, int argIdx) {
        PredInfo pred_info = rule.get(predIdx);
        return (null != pred_info.args[argIdx]) && (1 < varCntList.get(pred_info.args[argIdx].id));
    }
}
