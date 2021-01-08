package compressor;

import common.GraphNode4Compound;
import common.JplRule;
import org.jpl7.*;
import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.*;

public class CompressorWithMultisetDynamicRuleConstruction {
    private enum PrologModule {
        GLOBAL("global"), CURRENT("current"), START_SET("start_set");

        private final String sessionName;

        PrologModule(String sessionName) {
            this.sessionName = sessionName;
        }

        public String getSessionName() {
            return sessionName;
        }
    }

    enum ArgType {
        CONST, VAR
    }

    static class ArgInfo {
        final String name;
        final ArgType type;

        public ArgInfo(String name, ArgType type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArgInfo argInfo = (ArgInfo) o;
            return Objects.equals(name, argInfo.name) && type == argInfo.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }
    }

    static class PredInfo {
        final String predicate;
        final ArgInfo[] args;

        public PredInfo(String predicate, int arity) {
            this.predicate = predicate;
            args = new ArgInfo[arity];
        }
    }

    static class RuleConstructor {

        static class ArgPair {
            final int predIdx1;
            final int argIdx1;
            final int predIdx2;
            final int argIdx2;

            public ArgPair(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
                this.predIdx1 = predIdx1;
                this.argIdx1 = argIdx1;
                this.predIdx2 = predIdx2;
                this.argIdx2 = argIdx2;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ArgPair argPair = (ArgPair) o;
                return predIdx1 == argPair.predIdx1 && argIdx1 == argPair.argIdx1 && predIdx2 == argPair.predIdx2 && argIdx2 == argPair.argIdx2;
            }

            @Override
            public int hashCode() {
                return Objects.hash(predIdx1, argIdx1, predIdx2, argIdx2);
            }
        }

        /* Head pred is at index 0; body from 1 to ... */
        int vars_cnt;
        final List<PredInfo> rule;
        final List<String> rulePredList;
        final List<MultiSet<String>[]> ruleArgSetList;
        final Map<String, MultiSet<String>[]> otherPred2ArgSetMap;
        final Set<ArgPair> knownArgPiars;

        public RuleConstructor(String headPred, int headPredArity, Map<String, MultiSet<String>[]> pred2ArgSetMap) {
            PredInfo head = new PredInfo(headPred, headPredArity);
//            for (int i = 0; i < headPredArity; i++) {
//                head.args[i] = new ArgInfo(String.format("X%d", i), ArgType.VAR);
//            }
            rule = new ArrayList<>();
            rule.add(head);
            vars_cnt = 0;

            rulePredList = new ArrayList<>();
            ruleArgSetList = new ArrayList<>();
            otherPred2ArgSetMap = new HashMap<>(pred2ArgSetMap);
            ruleArgSetList.add(otherPred2ArgSetMap.remove(headPred));
            knownArgPiars = new HashSet<>();
        }

        public boolean findCorrespondingArgs() throws Exception {
            /* 先比较规则中已有的谓词的参数 */
            double max_sim_rule_2_rule = 0;
            ArgPair max_rule_2_rul2_arg_pair = null;
            for (int i = 0; i < ruleArgSetList.size(); i++) {
                MultiSet<String>[] pred_arg_sets_i = ruleArgSetList.get(i);
                for (int j = i + 1; j < ruleArgSetList.size(); j++) {
                    MultiSet<String>[] pred_arg_sets_j = ruleArgSetList.get(j);
                    for (int arg_idx_i = 0; arg_idx_i < pred_arg_sets_i.length; arg_idx_i++) {
                        for (int arg_idx_j = 0; arg_idx_j < pred_arg_sets_j.length; arg_idx_j++) {
                            ArgPair arg_pair = new ArgPair(i, arg_idx_i, j, arg_idx_j);
                            if (!knownArgPiars.contains(arg_pair)) {
                                double similarity = pred_arg_sets_i[arg_idx_i]
                                        .jaccardSimilarity(pred_arg_sets_j[arg_idx_j]);
                                if (similarity > max_sim_rule_2_rule) {
                                    max_sim_rule_2_rule = similarity;
                                    max_rule_2_rul2_arg_pair = arg_pair;
                                }
                            }
                        }
                    }
                }
            }

            /* 再将规则中已有的谓词的参数和其他谓词参数进行比较 */
            class Rule2OtherArgPair {
                final int predIdx1;
                final int argIdx1;
                final String pred2;
                final int argIdx2;

                public Rule2OtherArgPair(int predIdx1, int argIdx1, String pred2, int argIdx2) {
                    this.predIdx1 = predIdx1;
                    this.argIdx1 = argIdx1;
                    this.pred2 = pred2;
                    this.argIdx2 = argIdx2;
                }
            }
            double max_sim_rule_2_other = 0;
            Rule2OtherArgPair max_rule_2_other_arg_pair = null;
            for (int i = 0; i < ruleArgSetList.size(); i++) {
                MultiSet<String>[] pred_arg_sets_i = ruleArgSetList.get(i);
                for (Map.Entry<String, MultiSet<String>[]> entry: otherPred2ArgSetMap.entrySet()) {
                    MultiSet<String>[] pred_arg_sets_j = entry.getValue();
                    for (int arg_idx_i = 0; arg_idx_i < pred_arg_sets_i.length; arg_idx_i++) {
                        for (int arg_idx_j = 0; arg_idx_j < pred_arg_sets_j.length; arg_idx_j++) {
                            double similarity = pred_arg_sets_i[arg_idx_i]
                                    .jaccardSimilarity(pred_arg_sets_j[arg_idx_j]);
                            if (similarity > max_sim_rule_2_other) {
                                max_sim_rule_2_other = similarity;
                                max_rule_2_other_arg_pair = new Rule2OtherArgPair(
                                        i, arg_idx_i, entry.getKey(), arg_idx_j
                                );
                            }
                        }
                    }
                }
            }

            if (null == max_rule_2_rul2_arg_pair && null == max_rule_2_other_arg_pair) {
                /* 比较后找不到可以匹配的参数了 */
                return false;
            }

            /* 将相似度最高的两个参数设置成同一个变量 */
            int pred_idx1;
            int pred_idx2;
            int arg_idx1;
            int arg_idx2;
            if (max_sim_rule_2_rule > max_sim_rule_2_other) {
                /* 相似度最高的两个变量都在已经构造好的rule中，直接将两个变量设置成同一个 */
                pred_idx1 = max_rule_2_rul2_arg_pair.predIdx1;
                pred_idx2 = max_rule_2_rul2_arg_pair.predIdx2;
                arg_idx1 = max_rule_2_rul2_arg_pair.argIdx1;
                arg_idx2 = max_rule_2_rul2_arg_pair.argIdx2;
            } else {
                /* 相似度最高的两个变量里一个在已经构造的rule中，另一个在还未排布的pred中 */
                /* 将新的pred放入rule */
                pred_idx1 = max_rule_2_other_arg_pair.predIdx1;
                pred_idx2 = rule.size();
                arg_idx1 = max_rule_2_other_arg_pair.argIdx1;
                arg_idx2 = max_rule_2_other_arg_pair.argIdx2;

                String new_pred = max_rule_2_other_arg_pair.pred2;
                MultiSet<String>[] new_pred_args_set = otherPred2ArgSetMap.remove(new_pred);
                PredInfo new_pred_info = new PredInfo(new_pred, new_pred_args_set.length);
                rule.add(new_pred_info);
                rulePredList.add(new_pred);
                ruleArgSetList.add(new_pred_args_set);
            }

            PredInfo pred_info1 = rule.get(pred_idx1);
            PredInfo pred_info2 = rule.get(pred_idx2);
            ArgInfo arg_info1 = pred_info1.args[arg_idx1];
            ArgInfo arg_info2 = pred_info2.args[arg_idx2];

            if (null == arg_info1) {
                if (null == arg_info2) {
                    /* 创建新的变量并将两者设置成已知 */
                    ArgInfo new_arg_var = new ArgInfo(String.format("X%d", vars_cnt++), ArgType.VAR);
                    pred_info1.args[arg_idx1] = new_arg_var;
                    pred_info2.args[arg_idx2] = new_arg_var;
                    knownArgPiars.add(new ArgPair(pred_idx1, arg_idx1, pred_idx2, arg_idx2));
                } else {
                    /* 将pred_idx1和arg_idx1对应的参数设置成对应的变量，并更新已知表 */
                    pred_info1.args[arg_idx1] = arg_info2;
                    for (int pred_idx = 0; pred_idx < rule.size(); pred_idx++) {
                        PredInfo pred_info = rule.get(pred_idx);
                        for (int arg_idx = 0; arg_idx < pred_info.args.length; arg_idx++) {
                            ArgInfo arg_info = pred_info.args[arg_idx];
                            if (arg_info == arg_info2) {
                                knownArgPiars.add(new ArgPair(pred_idx, arg_idx, pred_idx1, arg_idx1));
                                knownArgPiars.add(new ArgPair(pred_idx1, arg_idx1, pred_idx, arg_idx));
                            }
                        }
                    }
                }
            } else {
                if (null == arg_info2) {
                    /* 将pred_idx2和arg_idx2对应的参数设置成对应的变量，并更新已知表 */
                    pred_info2.args[arg_idx2] = arg_info1;
                    for (int pred_idx = 0; pred_idx < rule.size(); pred_idx++) {
                        PredInfo pred_info = rule.get(pred_idx);
                        for (int arg_idx = 0; arg_idx < pred_info.args.length; arg_idx++) {
                            ArgInfo arg_info = pred_info.args[arg_idx];
                            if (arg_info == arg_info1) {
                                knownArgPiars.add(new ArgPair(pred_idx, arg_idx, pred_idx2, arg_idx2));
                                knownArgPiars.add(new ArgPair(pred_idx2, arg_idx2, pred_idx, arg_idx));
                            }
                        }
                    }
                } else {
                    /* 两个都已经比对过，不应该参与比较，抛出异常 */
                    throw new Exception("两个已经比对过的参数又参与了比较");
                }
            }

            return true;
        }

        public void updateRuleArgSets() {
            List<Compound> rule_compounds = new ArrayList<>();
            /* 把head转成Prolog String */
            PredInfo head_pred_info = rule.get(0);
            Term[] args = new Term[head_pred_info.args.length];
            for (int i = 0; i < args.length; i++) {
                if (null == head_pred_info.args[i]) {
                    args[i] = new Variable("_");
                } else {
                    args[i] = new Variable(head_pred_info.args[i].name);
                }
            }
            Compound head_compound = new Compound(rulePredList.get(0), args);
            rule_compounds.add(head_compound);
            StringBuilder builder = new StringBuilder(head_compound.toString());

            /* 把body中的每一个pred都转成Prolog String拼接在后面，并记录所有的已经确定的参数 */
            for (int i = 1; i < rule.size(); i++) {
                PredInfo body_pred_info = rule.get(0);
                args = new Term[body_pred_info.args.length];
                for (int j = 0; j < args.length; j++) {
                    if (null == body_pred_info.args[j]) {
                        args[j] = new Variable("_");
                    } else {
                        args[j] = new Variable(body_pred_info.args[j].name);
                    }
                }
                Compound body_compound = new Compound(rulePredList.get(i), args);
                builder.append(',').append(body_compound.toString());
            }

            /* 匹配所有distinct的确定的参数的取值 */
            Query q = new Query(":", new Term[]{
                    new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(builder.toString())
            });
            Set<Map<String, Term>> unique_binding_set = new HashSet<>();
            for (Map<String, Term> binding: q) {
                unique_binding_set.add(binding);
            }
            q.close();
            for (int i = 0; i < rule.size(); i++) {
                PredInfo pred_info = rule.get(i);
                Compound compound = rule_compounds.get(i);
                int unknown_args_cnt = 0;
                for (int arg_idx = 0; arg_idx < compound.arity(); arg_idx++) {
                    if (compound.arg(arg_idx).name().equals("_")) {
                        compound.arg(arg_idx).setName(String.format("Y%d", unknown_args_cnt++));
                    }
                }
                Set<Compound> instanced_compound_set = new HashSet<>();
                for (Map<String, Term> binding: unique_binding_set) {
                    instanced_compound_set.add(substitute(compound, binding));
                }
                if (unknown_args_cnt > 0) {
                    /* 查询所有facts */
                    Set<Compound> query_compound_set = instanced_compound_set;
                    instanced_compound_set = new HashSet<>();
                    for (Compound query_compound: query_compound_set) {
                        q = new Query(":", new Term[]{
                                new Atom(PrologModule.GLOBAL.getSessionName()), query_compound
                        });
                        for (Map<String, Term> binding: unique_binding_set) {
                            instanced_compound_set.add(substitute(query_compound, binding));
                        }
                        q.close();
                    }
                }
                MultiSet<String>[] arg_sets = ruleArgSetList.get(i);
                for (int set_idx = 0; set_idx < arg_sets.length; set_idx++) {
                    arg_sets[set_idx] = new MultiSet<>();
                }
                for (Compound fact: instanced_compound_set) {
                    for (int arg_idx = 0; arg_idx < fact.arity(); arg_idx++) {
                        arg_sets[arg_idx].add(fact.arg(arg_idx+1).name());
                    }
                }
            }
        }

        private Compound substitute(Compound compound, Map<String, Term> binding) {
            Term[] bounded_args = new Term[compound.arity()];
            for (int i = 0; i < bounded_args.length; i++) {
                Term original = compound.arg(i+1);
                bounded_args[i] = binding.getOrDefault(original.name(), original);
            }
            return new Compound(compound.name(), bounded_args);
        }

        public JplRule findRule() throws Exception {
            /* Find all argument settings */
            while (findCorrespondingArgs()) {
                updateRuleArgSets();
            }

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
            Compound head_compound = new Compound(rulePredList.get(0), args);

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
                body_compounds[pred_idx - 1] = new Compound(rulePredList.get(pred_idx), args);
            }

            return new JplRule(head_compound, body_compounds);
        }
    }

    private final String bkPath;
    private final String hypothesisPath;
    private final boolean debug;

    private final Set<Compound> originalFacts = new HashSet<>();
//    private final Set<Compound> currentFacts = new HashSet<>();
    private final Map<String, Set<Compound>> curFactsByPred = new HashMap<>();
    private final Map<String, MultiSet<String>[]> pred2ArgSetMap = new HashMap<>();
    private final Map<String, Integer> pred2ArityMap = new HashMap<>();

    private final List<JplRule> rules = new ArrayList<>();
    private final Map<GraphNode4Compound, Set<GraphNode4Compound>> graph = new HashMap<>();
    private final Set<Compound> counterExamples = new HashSet<>();
    private final Set<Compound> startSet = new HashSet<>();

    public CompressorWithMultisetDynamicRuleConstruction(String bkPath, String hypothesisPath, boolean debug) {
        this.bkPath = bkPath;
        this.hypothesisPath = hypothesisPath;
        this.debug = debug;
    }

    private void appendKnowledge(PrologModule module, Term knowledge) {
        Query q = new Query(
                new Compound(":", new Term[]{
                        new Atom(module.getSessionName()), new Compound("assertz", new Term[]{knowledge})
                })
        );
        q.hasSolution();
        q.close();
    }

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     *
     * Assume no duplication.
     */
    private void loadBk() {
        try {
            System.out.println("\n>>> Loading BK...");
            BufferedReader reader = new BufferedReader(new FileReader(bkPath));
            String line;
            int line_cnt = 0;
            while (null != (line = reader.readLine())) {
                String[] components = line.split("\t");
                MultiSet<String>[] arg_set_list =  pred2ArgSetMap.computeIfAbsent(components[0], k -> {
                    MultiSet<String>[] _arg_set_list = new MultiSet[components.length - 1];
                    for (int i = 0; i < _arg_set_list.length; i++) {
                        _arg_set_list[i] = new MultiSet<>();
                    }
                    return _arg_set_list;
                });
                Atom[] args = new Atom[components.length - 1];
                for (int i = 1; i < components.length; i++) {
                    arg_set_list[i-1].add(components[i]);
                    args[i-1] = new Atom(components[i]);
                }
                String predicate = components[0];
                Compound compound = new Compound(predicate, args);
                appendKnowledge(PrologModule.GLOBAL, compound);
                originalFacts.add(compound);
                appendKnowledge(PrologModule.CURRENT, compound);
                curFactsByPred.computeIfAbsent(predicate, k -> new HashSet<>()).add(compound);
                line_cnt++;
            }
            for (Map.Entry<String, MultiSet<String>[]> entry: pred2ArgSetMap.entrySet()) {
                pred2ArityMap.put(entry.getKey(), entry.getValue().length);
            }
            System.out.printf("BK loaded: %d predicates; %d facts\n", pred2ArgSetMap.size(), line_cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JplRule generateRule() throws Exception {
        /* 找到当前数量最多的Predicate为Head */
        String max_pred = null;
        int max_cnt = 0;
        for (Map.Entry<String, Set<Compound>> entry: curFactsByPred.entrySet()) {
            if (entry.getValue().size() > max_cnt) {
                max_cnt = entry.getValue().size();
                max_pred = entry.getKey();
            }
        }

        /* 不断找和当前已经构造的规则中的自由参数最接近的参数进行绑定，找到规则 */
        RuleConstructor constructor = new RuleConstructor(max_pred, pred2ArityMap.get(max_pred), pred2ArgSetMap);
        JplRule rule = constructor.findRule();

        if (!acceptRule(rule)) {
            return null;
        }
        return rule;
    }

    private boolean acceptRule(JplRule rule) {
        /* Todo: Implement Here */
        return true;
    }

    private boolean shouldContinue() {
        /* Todo: Implement Here */
        return true;
    }

    public void run() throws Exception {
        loadBk();

        while (shouldContinue()) {
            JplRule rule = generateRule();
            System.out.println(rule);
            break;
        }
    }

    public static void main(String[] args) {
        CompressorWithMultisetDynamicRuleConstruction compressor = new CompressorWithMultisetDynamicRuleConstruction(
                "FamilyRelationSimple(0.05)(100x).tsv",
                "HypothesisDynamic.pl",
                true
        );
        try {
            compressor.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
