package compressor.estimation.condprob;

import common.JplRule;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;

import java.util.ArrayList;
import java.util.List;

public class RuleInfo {
    List<PredInfo> rule;
    double score;

    public RuleInfo() {
        rule = new ArrayList<>();
        score = 0;
    }

    public RuleInfo(RuleInfo another) {
        this.rule = new ArrayList<>(another.rule.size());
        for (PredInfo pred_info: another.rule) {
            this.rule.add(new PredInfo(pred_info));
        }
        this.score = another.score;
    }

    public JplRule toJplRule() {
        /* 如果仍有变量没有设定，则构造失败 */
        PredInfo head_pred_info = rule.get(0);
        Term[] args = new Term[head_pred_info.args.length];
        for (int i = 0; i < args.length; i++) {
            if (null == head_pred_info.args[i]) {
                return null;
            } else {
                args[i] = new Variable(head_pred_info.args[i].name);
            }
        }
        Compound head_compound = new Compound(head_pred_info.predicate, args);

        Compound[] body_compounds = new Compound[rule.size() - 1];
        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            PredInfo body_pred_info = rule.get(pred_idx);
            args = new Term[body_pred_info.args.length];
            for (int arg_idx = 0; arg_idx < args.length; arg_idx++) {
                if (null == body_pred_info.args[arg_idx]) {
                    return null;
                } else {
                    args[arg_idx] = new Variable(body_pred_info.args[arg_idx].name);
                }
            }
            body_compounds[pred_idx - 1] = new Compound(body_pred_info.predicate, args);
        }

        return new JplRule(head_compound, body_compounds);
    }
}
