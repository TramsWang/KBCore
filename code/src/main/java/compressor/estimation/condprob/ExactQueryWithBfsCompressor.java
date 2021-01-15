package compressor.estimation.condprob;

import common.JplRule;
import org.jpl7.Compound;
import utils.MultiSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExactQueryWithBfsCompressor extends ExactQueryWithHeapCompressor {

    protected static final double MIN_SCORE = 0.5;

    public ExactQueryWithBfsCompressor(
            String kbFilePath, String hypothesisFilePath, String startSetFilePath, String counterExampleSetFilePath,
            boolean debug
    ) {
        super(kbFilePath, hypothesisFilePath, startSetFilePath, counterExampleSetFilePath, debug);
    }

    @Override
    protected JplRule findRule() {
        List<RuleInfo> candidates = new ArrayList<>();
        for (Map.Entry<String, Set<Compound>> entry: curPred2FactSetMap.entrySet()) {
            String predicate = entry.getKey();
            Set<Compound> fact_set = entry.getValue();
            int arity = pred2ArityMap.get(predicate);
            RuleInfo rule_info = new RuleInfo(predicate, arity);
            rule_info.score = scoreMetric(fact_set.size(), Math.pow(constants.size(), arity));
            candidates.add(rule_info);
        }

        for (int i = 0; i < candidates.size(); i++) {
            /* 如果得分最高的规则是一条完整的rule，则直接返回 */
            RuleInfo rule_info = candidates.get(i);
            JplRule jpl_rule = rule_info.toJplRule();
            if (null != jpl_rule && MIN_SCORE < rule_info.score) {
                System.out.printf(">>> %s\n", jpl_rule);
                return jpl_rule;
            }

            /* 遍历当前rule所有的可能的一步扩展，并加入heap */
            System.out.printf("Extend: %s\n", rule_info.toString());
            int pred_idx = rule_info.ruleSize() - 1;
            PredInfo last_pred_info = rule_info.getPred(pred_idx);
            int arg_idx;
            for (arg_idx = 0;
                 arg_idx < last_pred_info.args.length && null != last_pred_info.args[arg_idx];
                 arg_idx++
            ) {}
            if (arg_idx < last_pred_info.args.length) {
                /* 有未知参数，先解决未知参数 */
                for (int var_id = 0; var_id <= rule_info.getVarCnt(); var_id++) {
                    RuleInfo new_rule_info = new RuleInfo(rule_info);
                    new_rule_info.setUnknownArg(pred_idx, arg_idx, var_id);
                    new_rule_info.score = calRuleScore(new_rule_info);
                    if (!Double.isNaN(new_rule_info.score)) {
                        candidates.add(new_rule_info);
                    }
                }
            } else {
                /* 没有未知参数，但是有自由变量，创建新的predicate */
                for (Map.Entry<String, Integer> entry: pred2ArityMap.entrySet()) {
                    RuleInfo new_rule_info = new RuleInfo(rule_info);
                    new_rule_info.addNewPred(entry.getKey(), entry.getValue());
                    new_rule_info.score = rule_info.score;
                    if (!Double.isNaN(new_rule_info.score)) {
                        candidates.add(new_rule_info);
                    }
                }
            }
        }

        return null;
    }

    public static void main(String[] args) {
        ExactQueryWithBfsCompressor compressor = new ExactQueryWithBfsCompressor(
                "FamilyRelationSimple(0.05)(100x).tsv",
                null,
                null,
                null,
                true
        );
        compressor.run();
    }
}
