package compressor.ml.locopt;

import common.JplRule;
import compressor.ml.CompressorBase;
import compressor.ml.PredInfo;
import compressor.ml.SwiplUtil;
import compressor.ml.PrologModule;
import org.jpl7.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.*;

public class ExactQueryCompressor extends CompressorBase<RuleInfo> {

    protected static final double MIN_HEAD_COVERAGE = 0.05;
    protected static final double MIN_COMPRESSION_RATE = 0.5;

    protected final Set<Compound> globalFacts = new HashSet<>();
    protected final Map<String, Integer> pred2ArityMap = new HashMap<>();
    protected final Set<Compound> curFacts = new HashSet<>();
    protected final Map<String, Set<Compound>> curPred2FactSetMap = new HashMap<>();
    protected final Set<String> constants = new HashSet<>();
    protected final List<JplRule> hypothesis = new ArrayList<>();

    protected boolean shouldContinue = true;

    public ExactQueryCompressor(
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
    protected RuleInfo findRule() {
        Set<String> ignore_set = new HashSet<>();
        if (debug) {
            ignore_set.add("gender");
        }

        /* 找到仅有head的rule中得分最高的作为起始rule */
        List<RuleInfo> starting_rules = new ArrayList<>();
        for (Map.Entry<String, Integer> entry: pred2ArityMap.entrySet()) {
            String predicate = entry.getKey();
            Integer arity = entry.getValue();
            if (ignore_set.contains(predicate)) {
                continue;
            }
            RuleInfo rule_info = new RuleInfo(predicate, arity);
            checkThenAddRule(starting_rules, rule_info);
        }
        if (starting_rules.isEmpty()) {
            /* 没有适合条件的规则了 */
            return null;
        }
        RuleInfo r_max = starting_rules.get(0);
        for (RuleInfo r: starting_rules) {
            if (r.getEvalMetric().getEvaluation() > r_max.getEvalMetric().getEvaluation()) {
                r_max = r;
            }
        }

        /* 寻找局部最优（只要进入这个循环，一定有局部最优） */
        while (true) {
            System.out.printf("Extend: %s\n", r_max);
            RuleInfo r_e_max = r_max;

            /* 遍历r_max的后继邻居 */
            List<RuleInfo> successors = findSucc(r_max);
            for (RuleInfo successor: successors) {
                if (successor.getEvalMetric().getEvaluation() > r_e_max.getEvalMetric().getEvaluation()) {
                    r_e_max = successor;
                }
            }

            /* 遍历r_max的前驱邻居 */
            List<RuleInfo> predecessors = findPred(r_max);
            for (RuleInfo predecessor: predecessors) {
                if (predecessor.getEvalMetric().getEvaluation() > r_e_max.getEvalMetric().getEvaluation()) {
                    r_e_max = predecessor;
                }
            }

            if (r_e_max == r_max) {
                return r_max;
            }
            r_max = r_e_max;
        }
    }

    protected EvalMetric evalRule(RuleInfo ruleInfo) {
        /* 如果head上的所有变量都是自由变量则直接计算 */
        PredInfo head_pred_info =  ruleInfo.getHead();
        List<String> bounded_vars_in_head = new ArrayList<>();
        Term[] head_args = new Term[head_pred_info.args.length];
        int free_var_cnt_in_head = 0;
        for (int arg_idx = 0; arg_idx < head_args.length; arg_idx++) {
            if (null == head_pred_info.args[arg_idx]) {
                head_args[arg_idx] = new Variable(String.format("Y%d", free_var_cnt_in_head));
                free_var_cnt_in_head++;
            } else {
                head_args[arg_idx] = new Variable(head_pred_info.args[arg_idx].name);
                bounded_vars_in_head.add(head_pred_info.args[arg_idx].name);
            }
        }
        Set<Compound> facts = curPred2FactSetMap.get(head_pred_info.predicate);
        if (bounded_vars_in_head.isEmpty()) {
            ruleInfo.setEvalMetric(
                    new CompressRatio(
                            facts.size(), Math.pow(constants.size(), head_pred_info.arity()), ruleInfo.size()
                    )
            );
            return ruleInfo.getEvalMetric();
        }

        /* 计算entailments */
        Compound head_compound = new Compound(head_pred_info.predicate, head_args);
        StringBuilder query_builder = new StringBuilder(head_compound.toString());
        for (int pred_idx = 1; pred_idx < ruleInfo.predCnt(); pred_idx++) {
            PredInfo body_pred_info = ruleInfo.getPred(pred_idx);
            Term[] args = new Term[body_pred_info.arity()];
            for (int arg_idx = 0; arg_idx < body_pred_info.arity(); arg_idx++) {
                args[arg_idx] = (null == body_pred_info.args[arg_idx]) ? new Variable("_") :
                        new Variable(body_pred_info.args[arg_idx].name);
            }
            Compound body_compound = new Compound(body_pred_info.predicate, args);
            query_builder.append(',').append(body_compound.toString());
        }
        String query_str = query_builder.toString();

        Query q = new Query(":", new Term[]{
                new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(query_str)
        });
        Map<String, Term>[] bindings = q.allSolutions();
        q.close();

        int pos_cnt = 0;
        Set<Compound> head_templates = new HashSet<>();
        for (Map<String, Term> binding: bindings) {
            Term[] template_args = new Term[bounded_vars_in_head.size()];
            for (int arg_idx = 0; arg_idx < template_args.length; arg_idx++) {
                template_args[arg_idx] = binding.get(bounded_vars_in_head.get(arg_idx));
            }
            head_templates.add(new Compound("h", template_args));

            Compound head_instance = SwiplUtil.substitute(head_compound, binding);
            if (facts.contains(head_instance)) {
                pos_cnt++;
            }
        }

        /* 用HC剪枝 */
        double head_coverage = ((double) pos_cnt) / facts.size();
        if (MIN_HEAD_COVERAGE >= head_coverage) {
            ruleInfo.setEvalMetric(null);
            return null;
        }

        ruleInfo.setEvalMetric(
                new CompressRatio(
                        pos_cnt,
                        head_templates.size() * Math.pow(constants.size(), free_var_cnt_in_head),
                        ruleInfo.size()
                )
        );
        return ruleInfo.getEvalMetric();
    }

    protected List<RuleInfo> findSucc(RuleInfo ruleInfo) {
        List<RuleInfo> successors = new ArrayList<>();

        /* 先找到所有空白的参数 */
        List<int[]> vacant_list = new ArrayList<>();    // 空白参数记录表：{pred_idx, arg_idx}
        for (int pred_idx = 0; pred_idx < ruleInfo.predCnt(); pred_idx++) {
            PredInfo pred_info = ruleInfo.getPred(pred_idx);
            for (int arg_idx = 0; arg_idx < pred_info.arity(); arg_idx++) {
                if (null == pred_info.args[arg_idx]) {
                    vacant_list.add(new int[]{pred_idx, arg_idx});
                }
            }
        }

        /* 尝试增加已知变量 */
        for (int var_id = 0; var_id < ruleInfo.usedVars(); var_id++) {
            for (int[] vacant: vacant_list) {
                /* 尝试将已知变量填入空白参数 */
                RuleInfo new_rule_info = new RuleInfo(ruleInfo);
                new_rule_info.setEmptyArg2KnownVar(vacant[0], vacant[1], var_id);
                checkThenAddRule(successors, new_rule_info);
            }

            for (Map.Entry<String, Integer> entry: pred2ArityMap.entrySet()) {
                /* 拓展一个谓词，并尝试一个已知变量 */
                String predicate = entry.getKey();
                int arity = entry.getValue();
                int new_pred_idx = ruleInfo.predCnt();
                RuleInfo new_rule_template = new RuleInfo(ruleInfo);
                new_rule_template.addPred(predicate, arity);
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    RuleInfo new_rule_info = new RuleInfo(new_rule_template);
                    new_rule_info.setEmptyArg2KnownVar(new_pred_idx, arg_idx, var_id);
                    checkThenAddRule(successors, new_rule_info);
                }
            }
        }

        /* 找到两个位置尝试同一个新变量 */
        for (int i = 0; i < vacant_list.size(); i++) {
            /* 找到新变量的第一个位置 */
            int[] first_vacant = vacant_list.get(i);
            for (int j = i + 1; j < vacant_list.size(); j++) {
                /* 新变量的第二个位置可以是当前rule中的其他空位 */
                int[] second_vacant = vacant_list.get(j);
                RuleInfo new_rule_info = new RuleInfo(ruleInfo);
                new_rule_info.setEmptyArgs2NewVar(
                        first_vacant[0], first_vacant[1], second_vacant[0], second_vacant[1]
                );
                checkThenAddRule(successors, new_rule_info);
            }
            for (Map.Entry<String, Integer> entry: pred2ArityMap.entrySet()) {
                /* 新变量的第二个位置也可以是拓展一个谓词以后的位置 */
                String predicate = entry.getKey();
                int arity = entry.getValue();
                int new_pred_idx = ruleInfo.predCnt();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    RuleInfo new_rule_info = new RuleInfo(ruleInfo);
                    new_rule_info.addPred(predicate, arity);
                    new_rule_info.setEmptyArgs2NewVar(
                            first_vacant[0], first_vacant[1], new_pred_idx, arg_idx
                    );
                    checkThenAddRule(successors, new_rule_info);
                }
            }
        }

        return successors;
    }

    protected List<RuleInfo> findPred(RuleInfo ruleInfo) {
        List<RuleInfo> predecessors = new ArrayList<>();
        for (int pred_idx = 0; pred_idx < ruleInfo.predCnt(); pred_idx++) {
            /* 从Head开始删除可能会出现Head中全部是自由变量但是Body不为空的情况，按照定义来说，这种规则是不在搜索
               空间中的，但是实际eval的时候会将这种规则当做空的规则，因此不会成为r_max，也就不会影响搜索结果 */
            PredInfo pred_info = ruleInfo.getPred(pred_idx);
            for (int arg_idx = 0; arg_idx < pred_info.arity(); arg_idx++) {
                if (null != pred_info.args[arg_idx]) {
                    RuleInfo new_rule_info = new RuleInfo(ruleInfo);
                    new_rule_info.removeKnownVar(pred_idx, arg_idx);
                    checkThenAddRule(predecessors, new_rule_info);
                }
            }
        }

        return predecessors;
    }

    protected void checkThenAddRule(Collection<RuleInfo> collection, RuleInfo ruleInfo) {
        if (!ruleInfo.isInvalid() && null != evalRule(ruleInfo)) {
            collection.add(ruleInfo);
        }
    }

    @Override
    protected void updateKb(RuleInfo rule) {
        if (null == rule || !rule.getEvalMetric().useful()) {
            shouldContinue = false;
            return;
        }

        JplRule jpl_rule = rule.toJplRule();
        hypothesis.add(jpl_rule);
        writeHypothesis();

        /* 删除已经被证明的 */
        PredInfo head_pred_info = rule.getHead();
        Term[] head_args = new Term[head_pred_info.args.length];
        int free_var_cnt_in_head = 0;
        for (int arg_idx = 0; arg_idx < head_args.length; arg_idx++) {
            if (null == head_pred_info.args[arg_idx]) {
                head_args[arg_idx] = new Variable(String.format("Y%d", free_var_cnt_in_head));
                free_var_cnt_in_head++;
            } else {
                head_args[arg_idx] = new Variable(head_pred_info.args[arg_idx].name);
            }
        }
        Compound head_compound = new Compound(head_pred_info.predicate, head_args);
        StringBuilder query_builder = new StringBuilder(head_compound.toString());
        for (int pred_idx = 1; pred_idx < rule.predCnt(); pred_idx++) {
            PredInfo body_pred_info = rule.getPred(pred_idx);
            Term[] args = new Term[body_pred_info.arity()];
            for (int arg_idx = 0; arg_idx < body_pred_info.arity(); arg_idx++) {
                args[arg_idx] = (null == body_pred_info.args[arg_idx]) ? new Variable("_") :
                        new Variable(body_pred_info.args[arg_idx].name);
            }
            Compound body_compound = new Compound(body_pred_info.predicate, args);
            query_builder.append(',').append(body_compound.toString());
        }
        String query_str = query_builder.toString();
        Query q = new Query(":", new Term[]{
                new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(query_str)
        });

        Set<Compound> fact_set = curPred2FactSetMap.get(jpl_rule.head.name());
        int removed_cnt = 0;
        for (Map<String, Term> binding: q) {
            Compound head_instance = SwiplUtil.substitute(head_compound, binding);
            SwiplUtil.retractKnowledge(PrologModule.CURRENT, head_instance);
            removed_cnt += fact_set.remove(head_instance) ? 1 : 0;
        }
        System.out.printf("Update: %d removed\n", removed_cnt);
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
        ExactQueryCompressor compressor = new ExactQueryCompressor(
                "FamilyRelationMedium(0.00)(10x).tsv",
                null,
                null,
                null,
                true
        );
        compressor.run();
    }
}
