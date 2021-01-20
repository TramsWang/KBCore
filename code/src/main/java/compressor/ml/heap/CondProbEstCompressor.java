package compressor.ml.heap;

import common.JplRule;
import compressor.ml.CompressorBase;
import compressor.ml.SwiplUtil;
import org.jpl7.Atom;
import org.jpl7.Compound;
import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CondProbEstCompressor extends CompressorBase<JplRule> {

    public static final int BEAM_SEARCH_N = 3;

    private final Set<Compound> globalFacts = new HashSet<>();
    private final Set<String> constants = new HashSet<>();
    private final Map<String, MultiSet<String>[]> pred2ArgSetMap = new HashMap<>();
    /* 按照这样的顺序排列时: P1.Arg1, P1.Arg2, ..., P1.LastArg, P2.Arg1, ..., LastPred.LastArg
       各个Pred的Idx */
    private final Map<String, Integer> pred2IdxMap = new HashMap<>();
    private final List<String> predList = new ArrayList<>();
    private final List<JplRule> hypothesis = new ArrayList<>();

    private int totalArgs = 0;
    private boolean shouldContinue = true;

    public CondProbEstCompressor(
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
                MultiSet<String>[] arg_set_list = pred2ArgSetMap.get(predicate);
                if (null == arg_set_list) {
                    arg_set_list = new MultiSet[components.length - 1];
                    for (int i = 0; i < arg_set_list.length; i++) {
                        arg_set_list[i] = new MultiSet<>();
                    }
                    pred2ArgSetMap.put(predicate, arg_set_list);
                    predList.add(predicate);
                    pred2IdxMap.put(predicate, pred_idx);
                    pred_idx += arg_set_list.length;
                }

                Atom[] args = new Atom[components.length - 1];
                for (int i = 1; i < components.length; i++) {
                    constants.add(components[i]);
                    arg_set_list[i-1].add(components[i]);
                    args[i-1] = new Atom(components[i]);
                }
                Compound compound = new Compound(predicate, args);
                SwiplUtil.appendKnowledge(PrologModule.GLOBAL, compound);
                globalFacts.add(compound);
            }
            totalArgs = pred_idx + 1;

            System.out.printf(
                    "BK loaded: %d predicates; %d constants, %d facts\n",
                    pred2ArgSetMap.size(), constants.size(), globalFacts.size()
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
//        /* 计算条件概率矩阵 */
//        double[][] cond_prob_matrix = calCondProb();  // P(A | B) = matrix[B][A]
//
//        /* 初始化一个最大堆，每次扩展堆顶的元素，直到找到n条最好的rule */
//        PriorityQueue<RuleInfo> max_heap = new PriorityQueue<>(
//                Comparator.comparingDouble((RuleInfo e) -> e.score).reversed()
//        );
//        List<JplRule> top_rules = new ArrayList<>();
//        for (Map.Entry<String, MultiSet<String>[]> entry: pred2ArgSetMap.entrySet()) {
//            String predicate = entry.getKey();
//            MultiSet<String>[] arg_sets = entry.getValue();
//            RuleInfo rule_info = new RuleInfo(predicate, arg_sets.length);
//            /* 2 * |H[Θ]| - |B[Θ]| */
//            rule_info.score = 2.0 * arg_sets[0].size() - Math.pow(constants.size(), arg_sets.length);
//            max_heap.add(rule_info);
//        }
//        for (; top_rules.size() < BEAM_SEARCH_N; ) {
//            RuleInfo rule_info = max_heap.poll();
//            JplRule jpl_rule = rule_info.toJplRule();
//            if (null != jpl_rule) {
//                top_rules.add(jpl_rule);
//                continue;
//            }
//
//            /* 遍历当前rule所有的可能的一步扩展，并加入heap */
//            System.out.printf("Extend: %s\n", rule_info.toString());
//            if (0 < rule_info.getUnknownArgCnt()) {
//                /* 有未知参数，先解决未知参数 */
//                int pred_idx = rule_info.ruleSize() - 1;
//                PredInfo last_pred_info = rule_info.getPred(pred_idx);
//                int arg_idx;
//                for (arg_idx = 0;
//                     arg_idx < last_pred_info.args.length && null != last_pred_info.args[arg_idx];
//                     arg_idx++
//                ) {}
//                arg_idx--;
//                for (int var_id = 0; var_id <= rule_info.getVarCnt(); var_id++) {
//                    RuleInfo new_rule_info = new RuleInfo(rule_info);
//                    new_rule_info.setUnknownArg(pred_idx, arg_idx, var_id);
//                    new_rule_info.score = calRuleScore(new_rule_info);
//                    max_heap.add(new_rule_info);
//                }
//            } else {
//                /* 没有未知参数，但是有自由变量，创建新的predicate */
//                for (Map.Entry<String, MultiSet<String>[]> entry: pred2ArgSetMap.entrySet()) {
//                    RuleInfo new_rule_info = new RuleInfo(rule_info);
//                    new_rule_info.addNewPred(entry.getKey(), entry.getValue().length);
//                    new_rule_info.score = calRuleScore(new_rule_info);
//                    max_heap.add(new_rule_info);
//                }
//            }
//        }
//
//        /* TODO: 比较最好的n条rule，选择效果最好的输出 */
//        for (JplRule rule: top_rules) {
//            System.out.printf("Candidate: %s\n", rule);
//        }
//        hypothesis.add(top_rules.get(0));
//        return top_rules.get(0);
        return null;
    }

    private double[][] calCondProb() {
        double[][] matrix = new double[totalArgs - 1][];
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = new double[totalArgs];
        }

        for (int pred_idx_i = 0, pred_offset_i = 0; pred_idx_i < predList.size(); pred_idx_i++) {
            MultiSet<String>[] arg_sets_i = pred2ArgSetMap.get(predList.get(pred_idx_i));
            int size_i = arg_sets_i[0].size();
            for (int pred_idx_j = pred_idx_i, pred_offset_j = pred_offset_i; pred_idx_j < predList.size(); pred_idx_j++) {
                MultiSet<String>[] arg_sets_j = pred2ArgSetMap.get(predList.get(pred_idx_j));
                int size_j = arg_sets_j[0].size();
                for (int arg_idx_i = 0; arg_idx_i < arg_sets_i.length; arg_idx_i++) {
                    for (int arg_idx_j = 0; arg_idx_j < arg_sets_j.length; arg_idx_j++) {
                        MultiSet<String> union = arg_sets_i[arg_idx_i].union(arg_sets_j[arg_idx_j]);
                        int union_size = union.size();
                        matrix[pred_offset_i + arg_idx_i][pred_offset_j + arg_idx_j] = ((double) union_size) / size_i;
                        matrix[pred_offset_j + arg_idx_j][pred_offset_i + arg_idx_i] = ((double) union_size) / size_j;
                    }
                }
                pred_offset_j += arg_sets_j.length;
            }
            pred_offset_i += arg_sets_i.length;
        }

        return matrix;
    }

    private double calRuleScore(RuleInfo ruleInfo) {
        /* Todo: Implement Here */
        return 0;
    }

    @Override
    protected void updateKb(JplRule rule) {
        /* Todo: 删除已经被证明的 */
        shouldContinue = false;
    }

    @Override
    protected void writeHypothesis() {
        /* Todo: Implement Here */
    }

    @Override
    protected void writeStartSet() {
        /* Todo: Implement Here */
    }

    @Override
    protected void writeCounterExampleSet() {
        /* Todo: Implement Here */
    }
}
