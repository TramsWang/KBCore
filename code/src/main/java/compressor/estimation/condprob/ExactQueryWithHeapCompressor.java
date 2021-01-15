package compressor.estimation.condprob;

import common.JplRule;
import compressor.estimation.CompressorBase;
import org.jpl7.*;
import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.*;

public class ExactQueryWithHeapCompressor extends CompressorBase<JplRule> {

    protected static final double MIN_HEAD_COVERAGE = 0.05;

    protected final Set<Compound> globalFacts = new HashSet<>();
    protected final Map<String, Integer> pred2ArityMap = new HashMap<>();
    protected final Set<Compound> curFacts = new HashSet<>();
    protected final Map<String, Set<Compound>> curPred2FactSetMap = new HashMap<>();
    protected final Set<String> constants = new HashSet<>();
    protected final List<JplRule> hypothesis = new ArrayList<>();

    protected boolean shouldContinue = true;

    public ExactQueryWithHeapCompressor(
            String kbFilePath, String hypothesisFilePath, String startSetFilePath, String counterExampleSetFilePath,
            boolean debug
    ) {
        super(kbFilePath, hypothesisFilePath, startSetFilePath, counterExampleSetFilePath, debug);
    }

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     *
     * Assume no duplication.
     */
    @Override
    protected void loadBk() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bkFilePath));
            String line;
            int pred_idx = 0;
            while (null != (line = reader.readLine())) {
                String[] components = line.split("\t");
                String predicate = components[0];

                Atom[] args = new Atom[components.length - 1];
                for (int i = 1; i < components.length; i++) {
                    constants.add(components[i]);
                    args[i-1] = new Atom(components[i]);
                }
                Compound compound = new Compound(predicate, args);
                SwiplUtil.appendKnowledge(PrologModule.GLOBAL, compound);
                SwiplUtil.appendKnowledge(PrologModule.CURRENT, compound);
                globalFacts.add(compound);
                curFacts.add(compound);
                curPred2FactSetMap.compute(predicate, (k, v) -> {
                    if (null == v) {
                        v = new HashSet<>();
                    }
                    v.add(compound);
                    return v;
                });
            }

            for (Map.Entry<String, Set<Compound>> entry: curPred2FactSetMap.entrySet()) {
                pred2ArityMap.put(entry.getKey(), entry.getValue().iterator().next().arity());
            }

            System.out.printf(
                    "BK loaded: %d predicates; %d constants, %d facts\n",
                    curPred2FactSetMap.size(), constants.size(), globalFacts.size()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean shouldContinue() {
        return shouldContinue;
    }

    @Override
    protected JplRule findRule() {
        /* 初始化一个最大堆，每次扩展堆顶的元素，直到找到1条最好的rule */
//        PriorityQueue<RuleInfo> max_heap = new PriorityQueue<>(
//                Comparator.comparingDouble((RuleInfo e) -> e.score).reversed()
//        );
        Set<String> ignore_set = new HashSet<>();
        if (debug) {
            ignore_set.add("gender");
        }


        MaxHeap<RuleInfo> max_heap = new MaxHeap<>();
        for (Map.Entry<String, Set<Compound>> entry: curPred2FactSetMap.entrySet()) {
            String predicate = entry.getKey();
            if (ignore_set.contains(predicate)) {
                continue;
            }
            Set<Compound> facts = entry.getValue();
            int arity = pred2ArityMap.get(predicate);
            RuleInfo rule_info = new RuleInfo(predicate, arity);
            rule_info.score = scoreMetric(facts.size(), Math.pow(constants.size(), arity));
            max_heap.add(rule_info, rule_info.score);
        }

        while (!max_heap.isEmpty()) {
            /* 如果得分最高的规则是一条完整的rule，则直接返回 */
            RuleInfo rule_info = max_heap.poll();
            JplRule jpl_rule = rule_info.toJplRule();
            if (null != jpl_rule) {
                System.out.printf("Found >>> %s\n", jpl_rule);
                return jpl_rule;
            }

            /* 遍历当前rule所有的可能的一步扩展，并加入heap */
            System.out.printf("Extend(score=%f): %s\n", rule_info.score, rule_info.toString());
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
                    if (!Double.isNaN(new_rule_info.score) && new_rule_info.score >= rule_info.score) {
                        max_heap.add(new_rule_info, new_rule_info.score);
                    }
                }
            } else {
                /* 没有未知参数，但是有自由变量，创建新的predicate */
                String head_pred = rule_info.getHead().predicate;
                pred_idx++;
                arg_idx = 0;
                for (Map.Entry<String, Integer> entry: pred2ArityMap.entrySet()) {
                    if (!head_pred.equals(entry.getKey())) {
                        RuleInfo tmp_rule_info = new RuleInfo(rule_info);
                        tmp_rule_info.addNewPred(entry.getKey(), entry.getValue());
//                        tmp_rule_info.score = rule_info.score;
//                        if (!Double.isNaN(tmp_rule_info.score)) {
//                            max_heap.add(tmp_rule_info, tmp_rule_info.score);
//                        }
                        /* 直接添加一个参数 */
                        for (int var_id = 0; var_id <= tmp_rule_info.getVarCnt(); var_id++) {
                            RuleInfo new_rule_info = new RuleInfo(tmp_rule_info);
                            new_rule_info.setUnknownArg(pred_idx, arg_idx, var_id);
                            new_rule_info.score = calRuleScore(new_rule_info);
                            if (!Double.isNaN(new_rule_info.score) && new_rule_info.score >= tmp_rule_info.score) {
                                max_heap.add(new_rule_info, new_rule_info.score);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    protected double calRuleScore(RuleInfo ruleInfo) {
        /* 如果head上的所有变量都是自由变量则直接计算 */
        Set<String> non_free_var_in_head = ruleInfo.NonFreeVarSetInHead();
        PredInfo head_pred_info =  ruleInfo.getHead();
        int free_var_cnt_in_head = head_pred_info.arity() - non_free_var_in_head.size();
        Set<Compound> facts = curPred2FactSetMap.get(head_pred_info.predicate);
        int head_arity = pred2ArityMap.get(head_pred_info.predicate);
        int total_pos_cnt = facts.size();
        if (non_free_var_in_head.isEmpty()) {
            return scoreMetric(total_pos_cnt, Math.pow(constants.size(), head_arity));
        }

        /* 计算positive entailments */
        StringBuilder query_builder = new StringBuilder();
        for (int pred_idx = 1; pred_idx < ruleInfo.ruleSize(); pred_idx++) {
            PredInfo body_pred_info = ruleInfo.getPred(pred_idx);
            Term[] args = new Term[body_pred_info.arity()];
            for (int arg_idx = 0; arg_idx < body_pred_info.arity(); arg_idx++) {
                args[arg_idx] = ruleInfo.isNonFreeVar(pred_idx, arg_idx) ?
                        new Variable(body_pred_info.args[arg_idx].name) : new Variable("_");
            }
            Compound body_compound = new Compound(body_pred_info.predicate, args);
            query_builder.append(body_compound.toString()).append(',');
        }
        query_builder.deleteCharAt(query_builder.length() - 1);
        String query_str = query_builder.toString();

        Query q = new Query(":", new Term[]{
                new Atom(PrologModule.CURRENT.getSessionName()), Term.textToTerm(query_str)
        });
        Set<Map<String, Term>> head_binding_set = new HashSet<>();
        for (Map<String, Term> binding: q) {
            Map<String, Term> head_binding = new HashMap<>();
            for (String non_free_var: non_free_var_in_head) {
                head_binding.put(non_free_var, binding.get(non_free_var));
            }
            head_binding_set.add(head_binding);
        }
        q.close();

        int pos_cnt = 0;
        Term[] head_args = new Term[head_pred_info.args.length];
        for (int arg_idx = 0; arg_idx < head_args.length; arg_idx++) {
            head_args[arg_idx] = new Variable(head_pred_info.args[arg_idx].name);
        }
        Compound head_compound = new Compound(head_pred_info.predicate, head_args);
        if (0 == free_var_cnt_in_head) {
            for (Map<String, Term> head_binding : head_binding_set) {
                Compound head_instance = SwiplUtil.substitute(head_compound, head_binding);
                if (globalFacts.contains(head_instance)) {
                    pos_cnt++;
                }
            }
        } else {
            for (Map<String, Term> head_binding : head_binding_set) {
                Compound head_template = SwiplUtil.substitute(head_compound, head_binding);
                Query sub_q = new Query(":", new Term[]{
                        new Atom(PrologModule.CURRENT.getSessionName()), head_template
                });
                for (Map<String, Term> template_binding : sub_q) {
                    Compound head_instance = SwiplUtil.substitute(head_template, template_binding);
                    if (globalFacts.contains(head_instance)) {
                        pos_cnt++;
                    }
                }
                sub_q.close();
            }
        }

        double head_coverage = ((double) pos_cnt) / total_pos_cnt;
        if (MIN_HEAD_COVERAGE >= head_coverage) {
            return Double.NaN;
        }

        /* 计算all possible entailments */
        double all_entailment_cnt = head_binding_set.size() * Math.pow(
                constants.size(), free_var_cnt_in_head
        );

        return scoreMetric(pos_cnt, all_entailment_cnt);
    }

    protected double scoreMetric(double posCnt, double allCnt) {
        return posCnt / allCnt;
    }

    @Override
    protected void updateKb(JplRule rule) {
        if (null == rule) {
            shouldContinue = false;
            return;
        }

        /* 删除已经被证明的 */
        hypothesis.add(rule);
        Set<Compound> fact_set = curPred2FactSetMap.get(rule.head.name());
        String query_str = String.format("%s,%s",rule.getHeadString(), rule.getBodyString());
        Query q = new Query(":", new Term[]{
                new Atom(PrologModule.CURRENT.getSessionName()), Term.textToTerm(query_str)
        });
        for (Map<String, Term> binding: q) {
            Compound head_instance = SwiplUtil.substitute(rule.head, binding);
            SwiplUtil.retractKnowledge(PrologModule.CURRENT, head_instance);
            fact_set.remove(head_instance);
        }
        q.close();
    }

    @Override
    protected void writeHypothesis() {
        System.out.println("\nHypothesis Found:");
        for (JplRule rule: hypothesis) {
            System.out.println(rule);
        }
    }

    @Override
    protected void writeStartSet() {
        /* Todo: Implement Here */
    }

    @Override
    protected void writeCounterExampleSet() {
        /* Todo: Implement Here */
    }

    public static void main(String[] args) {
        ExactQueryWithHeapCompressor compressor = new ExactQueryWithHeapCompressor(
                "FamilyRelationMedium(0.00)(10x).tsv",
                null,
                null,
                null,
                true
        );
        compressor.run();
    }
}
