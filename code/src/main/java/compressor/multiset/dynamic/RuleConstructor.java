package compressor.multiset.dynamic;

import common.JplRule;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;
import utils.MultiSet;

import java.util.*;

public class RuleConstructor {

    private static final double THRESHOLD = 0.3;

    /* Head pred is at index 0; body from 1 to ... */
    int vars_cnt;
    final List<PredInfo> rule;
    final List<MultiSet<String>[]> ruleArgSetList;
    final Map<String, MultiSet<String>[]> otherPred2ArgSetMap;

    public RuleConstructor(String headPred, int headPredArity, Map<String, MultiSet<String>[]> pred2ArgSetMap) {
        PredInfo head = new PredInfo(headPred, headPredArity);
        rule = new ArrayList<>();
        rule.add(head);
        vars_cnt = 0;

        ruleArgSetList = new ArrayList<>();
        otherPred2ArgSetMap = new HashMap<>(pred2ArgSetMap);
        ruleArgSetList.add(otherPred2ArgSetMap.remove(headPred));
    }

    public RuleConstructor(
            int vars_cnt, List<PredInfo> rule, List<MultiSet<String>[]> ruleArgSetList,
            Map<String, MultiSet<String>[]> otherPred2ArgSetMap
    ) {
        this.vars_cnt = vars_cnt;
        this.rule = rule;
        this.ruleArgSetList = ruleArgSetList;
        this.otherPred2ArgSetMap = otherPred2ArgSetMap;
    }

    public List<JplRule> findRules() throws Exception {
        List<JplRule> result = new ArrayList<>();

        /* 如果rule中已经填满了变量则输出这条rule */
        JplRule jpl_rule = convertRule();
        if (null != jpl_rule) {
            System.out.println(jpl_rule);
            result.add(jpl_rule);
            return result;
        }

        /* 先比较规则中已有的谓词的参数 */
        List<SimilarityInfo> sim_info_list = new ArrayList<>();
        for (int pred_idx_i = 0; pred_idx_i < ruleArgSetList.size(); pred_idx_i++) {
            PredInfo pred_info_i = rule.get(pred_idx_i);
            MultiSet<String>[] pred_arg_sets_i = ruleArgSetList.get(pred_idx_i);
            for (int pred_idx_j = pred_idx_i + 1; pred_idx_j < ruleArgSetList.size(); pred_idx_j++) {
                PredInfo pred_info_j = rule.get(pred_idx_j);
                MultiSet<String>[] pred_arg_sets_j = ruleArgSetList.get(pred_idx_j);
                for (int arg_idx_i = 0; arg_idx_i < pred_arg_sets_i.length; arg_idx_i++) {
                    for (int arg_idx_j = 0; arg_idx_j < pred_arg_sets_j.length; arg_idx_j++) {
                        if (null == pred_info_i.args[arg_idx_i] || null == pred_info_j.args[arg_idx_j]) {
                            double similarity = pred_arg_sets_i[arg_idx_i]
                                    .jaccardSimilarity(pred_arg_sets_j[arg_idx_j]);
                            sim_info_list.add(new SimilarityInfo(
                                    similarity, pred_idx_i, arg_idx_i, pred_idx_j, null, arg_idx_j
                            ));
                        }
                    }
                }
            }
        }

        /* 再将规则中已有的谓词的参数和其他谓词参数进行比较 */
        for (int pred_idx_i = 0; pred_idx_i < ruleArgSetList.size(); pred_idx_i++) {
            MultiSet<String>[] pred_arg_sets_i = ruleArgSetList.get(pred_idx_i);
            for (Map.Entry<String, MultiSet<String>[]> entry: otherPred2ArgSetMap.entrySet()) {
                MultiSet<String>[] pred_arg_sets_j = entry.getValue();
                for (int arg_idx_i = 0; arg_idx_i < pred_arg_sets_i.length; arg_idx_i++) {
                    for (int arg_idx_j = 0; arg_idx_j < pred_arg_sets_j.length; arg_idx_j++) {
                        double similarity = pred_arg_sets_i[arg_idx_i]
                                .jaccardSimilarity(pred_arg_sets_j[arg_idx_j]);
                        sim_info_list.add(new SimilarityInfo(
                                similarity, pred_idx_i, arg_idx_i, 0, entry.getKey(), arg_idx_j
                        ));
                    }
                }
            }
        }

        /* 给相似度排序，从高到低依次用来构造规则 */
        sim_info_list.sort(Comparator.comparingDouble((SimilarityInfo e) -> e.similarity).reversed());
        if (THRESHOLD > sim_info_list.get(0).similarity) {
            /* 如果所有的参数效果都不好，中止 */
            return result;
        }
        for (SimilarityInfo sim_info: sim_info_list) {
            /* 将参数复制一份 */
            int tmp_var_cnt = vars_cnt;
            List<PredInfo> tmp_rule = new ArrayList<>(rule.size());
            for (PredInfo pred_info: rule) {
                tmp_rule.add(new PredInfo(pred_info));
            }
            List<MultiSet<String>[]> tmp_rule_arg_set_list = new ArrayList<>(ruleArgSetList.size());
            for (MultiSet<String>[] arg_sets: ruleArgSetList) {
                tmp_rule_arg_set_list.add(arg_sets.clone());
            }
            Map<String, MultiSet<String>[]> tmp_other_pred_2_arg_set_map = new HashMap<>(otherPred2ArgSetMap);

            /* 将对应的两个参数设置成同一个变量 */
            PredInfo pred_info1;
            PredInfo pred_info2;
            ArgInfo arg_info1;
            ArgInfo arg_info2;
            if (null == sim_info.pred2) {
                /* 两个对应的参数都在当前的rule中 */
                pred_info1 = tmp_rule.get(sim_info.predIdx1);
                pred_info2 = tmp_rule.get(sim_info.predIdx2);
                arg_info1 = pred_info1.args[sim_info.argIdx1];
                arg_info2 = pred_info2.args[sim_info.argIdx2];
            } else {
                /* 两个对应的参数里一个在已经构造的rule中，另一个在还未排布的pred中 */
                pred_info1 = tmp_rule.get(sim_info.predIdx1);

                MultiSet<String>[] new_pred_args_set = tmp_other_pred_2_arg_set_map.remove(sim_info.pred2);
                pred_info2 = new PredInfo(sim_info.pred2, new_pred_args_set.length);
                tmp_rule.add(pred_info2);
                tmp_rule_arg_set_list.add(new_pred_args_set);

                arg_info1 = pred_info1.args[sim_info.argIdx1];
                arg_info2 = pred_info2.args[sim_info.argIdx2];
            }
            if (null == arg_info1) {
                if (null == arg_info2) {
                    /* 创建新的变量并将两者设置成已知 */
                    ArgInfo new_arg_var = new ArgInfo(String.format("X%d", tmp_var_cnt++), ArgType.VAR);
                    pred_info1.args[sim_info.argIdx1] = new_arg_var;
                    pred_info2.args[sim_info.argIdx2] = new_arg_var;
                } else {
                    /* 将pred_idx1和arg_idx1对应的参数设置成对应的变量，并更新已知表 */
                    pred_info1.args[sim_info.argIdx1] = arg_info2;
                }
            } else {
                if (null == arg_info2) {
                    /* 将pred_idx2和arg_idx2对应的参数设置成对应的变量，并更新已知表 */
                    pred_info2.args[sim_info.argIdx2] = arg_info1;
                } else {
                    /* 两个都已经比对过，不应该参与比较，抛出异常 */
                    throw new Exception("两个已经比对过的参数又参与了比较");
                }
            }

            /* Todo: 更新参数multiset */
            ?

            RuleConstructor next_step = new RuleConstructor(
                    tmp_var_cnt, tmp_rule, tmp_rule_arg_set_list, tmp_other_pred_2_arg_set_map
            );
            result.addAll(next_step.findRules());
        }

        return result;
    }

    private JplRule convertRule() {
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
