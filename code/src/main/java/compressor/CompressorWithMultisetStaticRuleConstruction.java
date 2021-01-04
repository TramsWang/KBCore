package compressor;

import common.GraphNode4Compound;
import common.JplRule;
import org.jpl7.*;
import utils.MultiSet;
import utils.graph.Tarjan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Integer;
import java.util.*;

public class CompressorWithMultisetStaticRuleConstruction {
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

    static class SimilarityInfo {
        double similarity;
        String pred1;
        int pred1ArgIdx;
        String pred2;
        int pred2ArgIdx;

        public SimilarityInfo(double similarity, String pred1, int pred1ArgIdx, String pred2, int pred2ArgIdx) {
            this.similarity = similarity;
            this.pred1 = pred1;
            this.pred1ArgIdx = pred1ArgIdx;
            this.pred2 = pred2;
            this.pred2ArgIdx = pred2ArgIdx;
        }
    }

    static class RuleComposer {
        static class PredArg {
            public final String predicate;
            public final int argIdx;

            public PredArg(String predicate, int argIdx) {
                this.predicate = predicate;
                this.argIdx = argIdx;
            }
        }

        private String headPredicate;
        private final Map<String, Integer> predicate2ArgCntMap;
        private final Compound head;
        private int unlinkedArgCnt;
        private Set<PredArg>[] headArgLinks;

        public RuleComposer(
                String headPredicate, Map<String, Integer> predicate2ArgCntMap
        ) {
            this.headPredicate = headPredicate;
            this.predicate2ArgCntMap = predicate2ArgCntMap;
            this.unlinkedArgCnt = predicate2ArgCntMap.get(headPredicate);
            headArgLinks = new Set[unlinkedArgCnt];

            Variable[] vars = new Variable[predicate2ArgCntMap.get(headPredicate)];
            for (int i = 0; i < vars.length; i++) {
                vars[i] = new Variable(String.format("X%d", i));
            }
            head = new Compound(headPredicate, vars);
        }

        public boolean linkArg(int headArgIdx, String bodyPredicate, int bodyPredicateIdx) {
            if (null == headArgLinks[headArgIdx]) {
                headArgLinks[headArgIdx] = new HashSet<>();
                unlinkedArgCnt--;
            }
            headArgLinks[headArgIdx].add(new PredArg(bodyPredicate, bodyPredicateIdx));
            return 0 == unlinkedArgCnt;
        }

        public List<JplRule> compose() {
            if (0 != unlinkedArgCnt) {
                return null;
            }
            List<JplRule> rules = new ArrayList<>();
            composeHandler(rules, new Stack<>());
            return rules;
        }

        private void composeHandler(List<JplRule> rules, Stack<PredArg> stack) {
            int idx = stack.size();
            if (idx >= headArgLinks.length) {
                Map<String, int[]> body_pred_map = new HashMap<>();
                for (int i = 0; i < headArgLinks.length; i++) {
                    PredArg pred_arg = stack.get(i);
                    int[] arg_links = body_pred_map.computeIfAbsent(
                            pred_arg.predicate,
                            k -> new int[predicate2ArgCntMap.get(k)]
                    );
                    arg_links[pred_arg.argIdx] = i + 1;
                }
                Map.Entry<String, int[]>[] body_preds = body_pred_map.entrySet().toArray(new Map.Entry[0]);

                /* 对所有未绑定的变量安排变量 */
                List<int[]> free_vars_list = new ArrayList<>();  // <pred idx, arg idx>
                for (int i = 0; i < body_preds.length; i++) {
                    int[] args = body_preds[i].getValue();
                    for (int j = 0; j < args.length; j++) {
                        if (0 == args[j]) {
                            free_vars_list.add(new int[]{i, j});
                        }
                    }
                }
                int free_var_cnt = free_vars_list.size() / 2;
                arrangeFreeVars(
                        rules, body_preds, free_vars_list, 0, headArgLinks.length + free_var_cnt
                );
                return;
            }

            for (PredArg pred_arg: headArgLinks[idx]) {
                stack.push(pred_arg);
                composeHandler(rules, stack);
                stack.pop();
            }
        }

        private void arrangeFreeVars(
                List<JplRule> rules, Map.Entry<String, int[]>[] bodyPreds, List<int[]> freeVarList,
                int idx, int totalVars
        ) {
            if (idx >= freeVarList.size()) {
                /* 生成一条rule */
                List<Compound> body_list = new ArrayList<>(bodyPreds.length);
                for (Map.Entry<String, int[]> entry: bodyPreds) {
                    String predicate = entry.getKey();
                    int[] var_idx = entry.getValue();
                    Variable[] vars = new Variable[predicate2ArgCntMap.get(predicate)];
                    for (int i = 0; i < vars.length; i++) {
                        vars[i] = new Variable(String.format("X%d", var_idx[i] - 1));
                    }
                    body_list.add(new Compound(predicate, vars));
                }
                JplRule rule = new JplRule(head, body_list.toArray(new Compound[0]));
                rules.add(rule);
                return;
            }

            int[] free_var_paras = freeVarList.get(idx);
            int[] body_pred_args = bodyPreds[free_var_paras[0]].getValue();
            int body_pred_arg_idx = free_var_paras[1];
            for (int i = 1; i <= totalVars; i++) {
                body_pred_args[body_pred_arg_idx] = i;
                arrangeFreeVars(rules, bodyPreds, freeVarList, idx+1, totalVars);
            }
        }
    }

    private final String bkPath;
    private final String hypothesisPath;
    private final boolean debug;

    private final Set<Compound> originalFacts = new HashSet<>();
    private final Set<Compound> currentFacts = new HashSet<>();
    private final Map<String, MultiSet<String>[]> predicate2ArgumentSetMap = new HashMap<>();
    private final Map<String, Integer> predicate2ArgsCntMap = new HashMap<>();
    private final List<JplRule> rules = new ArrayList<>();
    private final Map<GraphNode4Compound, Set<GraphNode4Compound>> graph = new HashMap<>();
    private final Set<Compound> counterExamples = new HashSet<>();
    private final Set<Compound> startSet = new HashSet<>();

    public CompressorWithMultisetStaticRuleConstruction(String bkPath, String hypothesisPath, boolean debug) {
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
                MultiSet<String>[] arg_set_list =  predicate2ArgumentSetMap.computeIfAbsent(components[0], k -> {
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
                Compound compound = new Compound(components[0], args);
                appendKnowledge(PrologModule.GLOBAL, compound);
                originalFacts.add(compound);
                appendKnowledge(PrologModule.CURRENT, compound);
                currentFacts.add(compound);
                line_cnt++;
            }
            for (Map.Entry<String, MultiSet<String>[]> entry: predicate2ArgumentSetMap.entrySet()) {
                predicate2ArgsCntMap.put(entry.getKey(), entry.getValue().length);
            }
            System.out.printf("BK loaded: %d predicates; %d facts\n", predicate2ArgumentSetMap.size(), line_cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<SimilarityInfo> computeJaccardSimilarities() {
        /* Compute all pairs of similarities */
        List<SimilarityInfo> similarity_info_list = new ArrayList<>();
        final int predicates_cnt = predicate2ArgumentSetMap.size();
        final String[] predicates = predicate2ArgumentSetMap.keySet().toArray(new String[0]);
        for (int i = 0; i < predicates_cnt; i++) {
            MultiSet<String>[] args_i = predicate2ArgumentSetMap.get(predicates[i]);
            int arity_i = args_i.length;
            for (int j = i + 1; j < predicates.length; j++) {
                MultiSet<String>[] args_j = predicate2ArgumentSetMap.get(predicates[j]);
                int arity_j = args_j.length;

                for (int ii = 0; ii < arity_i; ii++) {
                    for (int jj = 0; jj < arity_j; jj++) {
                        double similarity = args_i[ii].jaccardSimilarity(args_j[jj]);
                        similarity_info_list.add(
                                new SimilarityInfo(similarity, predicates[i], ii, predicates[j], jj)
                        );
                    }
                }
            }
        }

        /* Sort According to Jaccard Similarities */
        similarity_info_list.sort(
                Comparator.comparingDouble((SimilarityInfo e) -> e.similarity).reversed()
        );
        return similarity_info_list;
    }

    private JplRule generateRule(List<SimilarityInfo> similarityInfoList) {
        class CompactEdges {
            GraphNode4Compound src;
            List<GraphNode4Compound> dsts;

            public CompactEdges(GraphNode4Compound src, List<GraphNode4Compound> dsts) {
                this.src = src;
                this.dsts = dsts;
            }
        }

        Map<String, RuleComposer> composer_map = new HashMap<>();
        for (SimilarityInfo sim_info: similarityInfoList) {
            RuleComposer composer_pred1 = composer_map.computeIfAbsent(
                    sim_info.pred1,
                    k -> new RuleComposer(sim_info.pred1, predicate2ArgsCntMap)
            );
            boolean finished1 = composer_pred1.linkArg(sim_info.pred1ArgIdx, sim_info.pred2, sim_info.pred2ArgIdx);
            RuleComposer composer_pred2 = composer_map.computeIfAbsent(
                    sim_info.pred2,
                    k -> new RuleComposer(sim_info.pred2, predicate2ArgsCntMap)
            );
            boolean finished2 = composer_pred2.linkArg(sim_info.pred2ArgIdx, sim_info.pred1, sim_info.pred1ArgIdx);
            if (finished1 || finished2) {
                break;
            }
        }
        List<JplRule> rule_candidates = new ArrayList<>();
        for (RuleComposer composer: composer_map.values()) {
            List<JplRule> new_rules = composer.compose();
            if (null != new_rules) {
                rule_candidates.addAll(new_rules);
            }
        }

        /* Find the best rule to return */
        double score = 0.0;
        JplRule best_rule = new JplRule(null, new Compound[0]);
        List<CompactEdges> best_rule_edges = new ArrayList<>();
        Set<Compound> entailed_facts = new HashSet<>();
        Set<Compound> counter_examples = new HashSet<>();
        for (JplRule rule: rule_candidates) {
            Set<Compound> _entailed_facts = new HashSet<>();
            Set<Compound> _counter_examples = new HashSet<>();
            List<CompactEdges> _edges = new ArrayList<>();

            /* Instanciate a rule by binding its variables to each possible constant */
            StringBuilder builder = new StringBuilder();
            if (0 < rule.body.length) {
                builder.append(rule.body[0].toString());
                for (int j = 1; j < rule.body.length; j++) {
                    builder.append(',').append(rule.body[j].toString());
                }
                Query q = new Query(":", new Term[]{
                        new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(builder.toString())
                });
                for (Map<String, Term> binding: q) {
                    Compound head_substituted = substitute(rule.head, binding);
                    if (originalFacts.contains(head_substituted)) {
                        _entailed_facts.add(head_substituted);
                        CompactEdges comp_edges = new CompactEdges(
                                new GraphNode4Compound(head_substituted), new ArrayList<>()
                        );
                        for (Compound body: rule.body) {
                            Compound body_substituted = substitute(body, binding);
                            GraphNode4Compound body_node = new GraphNode4Compound(body_substituted);
                            comp_edges.dsts.add(body_node);
                        }
                        _edges.add(comp_edges);
                    } else {
                        _counter_examples.add(head_substituted);
                    }
                }
            }

            double _score = (_entailed_facts.size()) / (1.0 + _counter_examples.size());
            if ((_score > score) || (_score == score && rule.body.length < best_rule.body.length)){
                score = _score;
                best_rule = rule;
                best_rule_edges = _edges;
                entailed_facts = _entailed_facts;
                counter_examples = _counter_examples;
            }
        }

        if (0.5 < score) {
            for (CompactEdges comp_edge: best_rule_edges) {
                graph.computeIfAbsent(comp_edge.src, k -> new HashSet<>(comp_edge.dsts));
            }
            counterExamples.addAll(counter_examples);

            /* Remove Entailed Facts */
            currentFacts.removeAll(entailed_facts);
            for (Compound fact: entailed_facts) {
                predicate2ArgumentSetMap.computeIfPresent(fact.name(), (k, v) -> {
                    Term[] args = fact.args();
                    for (int i = 0; i < args.length; i++) {
                        v[i].remove(args[i].name());
                    }
                    return v;
                });
            }
            return best_rule;
        }
        return null;
    }

    private Compound substitute(Compound compound, Map<String, Term> binding) {
        Term[] bounded_args = new Term[compound.arity()];
        for (int i = 0; i < bounded_args.length; i++) {
            Term original = compound.arg(i+1);
            bounded_args[i] = binding.getOrDefault(original.name(), original);
        }
        return new Compound(compound.name(), bounded_args);
    }

    private void findCore() {
        System.out.println("\n>>> Finding Core...");
        /* 先把所有没有依赖（出边）的点加入START set */
        for (Compound fact: originalFacts) {
            GraphNode4Compound fact_node = new GraphNode4Compound(fact);
            if (!graph.containsKey(fact_node)) {
                startSet.add(fact);
            }
        }

        /* 再分析图上的所有强连通分量，把每一个强连通分量中的点都加入START set */
        final int old_start_set_size = startSet.size();
        Tarjan<GraphNode4Compound> tarjan = new Tarjan<>(graph);
        List<List<GraphNode4Compound>> sccs = tarjan.run();
        for (List<GraphNode4Compound> scc: sccs) {
            for (GraphNode4Compound node: scc) {
                startSet.add(node.compound);
            }
        }

        System.out.printf(
                "Core Found: %d in START set(%d SCCs; without SCC: %d); %d in COUNTER EXAMPLE set\n",
                startSet.size(), sccs.size(), old_start_set_size, counterExamples.size()
        );
    }

    private void writeHypothesis() {
        try (PrintWriter writer = new PrintWriter(hypothesisPath)) {
            for (JplRule rule: rules) {
                writer.println(rule);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        JPL.init();
        long time_start = System.currentTimeMillis();
        loadBk();
        long time_bk_loaded = System.currentTimeMillis();
        System.out.printf("Background Knowledge Loaded in %fs\n", (time_bk_loaded - time_start) / 1000.0);

        JplRule rule;
        System.out.println("Hypothesis:");
        while (0 < currentFacts.size() && null != (rule = generateRule(computeJaccardSimilarities()))) {
            System.out.println(rule);
            rules.add(rule);
        }
        long time_hypothesis_found = System.currentTimeMillis();
        System.out.printf("Hypothesis Found in %fs\n", (time_hypothesis_found - time_bk_loaded) / 1000.0);

        findCore();
        long time_core_found = System.currentTimeMillis();
        System.out.printf("Core found in %fs\n", (time_core_found - time_hypothesis_found) / 1000.0);
        System.out.printf("Total Time: %fs\n", (time_core_found - time_start) / 1000.0);

        writeHypothesis();
    }

    public static void main(String[] args) {
        CompressorWithMultisetStaticRuleConstruction compressor = new CompressorWithMultisetStaticRuleConstruction(
                "FamilyRelationMedium(0.05)(100x).tsv",
                "HypothesisFound.pl",
                false
        );
        compressor.run();
    }
}
