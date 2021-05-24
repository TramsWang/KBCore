package sinc.impl;

import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Term;
import sinc.SInC;
import sinc.common.*;
import sinc.util.MultiSet;
import sinc.util.RKB;
import sinc.util.graph.BaseGraphNode;
import sinc.util.graph.FeedbackVertexSetSolver;
import sinc.util.graph.Tarjan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class SincBasicWithRKB extends SInC<Predicate> {

    protected static final double MIN_HEAD_COVERAGE = 0.05;
    protected static final double MIN_CONSTANT_PROPORTION = 0.25;
    protected static final int DEFAULT_CONST_ID = -1;
    protected static final Predicate AXIOM = new Predicate("⊥", 0);

    protected final RKB kb = new RKB(null, MIN_HEAD_COVERAGE);
    protected final Map<String, Integer> functor2ArityMap = new HashMap<>();
    protected final Map<String, MultiSet<String>[]> curFunctor2ArgSetsMap = new HashMap<>();
    protected final Map<String, List<String>[]> functor2PromisingConstMap = new HashMap<>();

    protected final List<Rule> hypothesis = new ArrayList<>();
    protected boolean shouldContinue = true;
    protected List<String> waitingHeadFunctors = new ArrayList<>();

    protected final Map<Predicate, BaseGraphNode<Predicate>> predicate2NodeMap = new HashMap<>();
    protected final Map<BaseGraphNode<Predicate>, Set<BaseGraphNode<Predicate>>> graph = new HashMap<>();
    protected Set<Predicate> startSet;
    protected final Set<Predicate> counterExamples = new HashSet<>();

    public SincBasicWithRKB(int threadNum, int beamWidth, EvalMetric evalType, String bkFilePath, boolean debug) {
        super(threadNum, beamWidth, evalType, bkFilePath, debug);
    }

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     *
     * Assume no duplication.
     */
    @Override
    protected int loadBk() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bkFilePath));
            String line;
            while (null != (line = reader.readLine())) {
                String[] components = line.split("\t");
                String functor = components[0];
                int arity = components.length - 1;
                Predicate predicate = new Predicate(functor, arity);

                MultiSet<String>[] arg_set_list = curFunctor2ArgSetsMap.get(functor);
                if (null == arg_set_list) {
                    arg_set_list = new MultiSet[components.length - 1];
                    for (int i = 0; i < arg_set_list.length; i++) {
                        arg_set_list[i] = new MultiSet<>();
                    }
                    curFunctor2ArgSetsMap.put(functor, arg_set_list);
                    kb.defineFunctor(functor, arity);
                }

                for (int i = 1; i < components.length; i++) {
                    arg_set_list[i-1].add(components[i]);
                    predicate.args[i-1] = new Constant(DEFAULT_CONST_ID, components[i]);
                }
                kb.addPredicate(predicate);
            }

            for (Map.Entry<String, MultiSet<String>[]> entry: curFunctor2ArgSetsMap.entrySet()) {
                functor2ArityMap.put(entry.getKey(), entry.getValue().length);
            }

            /* 计算所有符合阈值的constant */
            for (Map.Entry<String, MultiSet<String>[]> entry: curFunctor2ArgSetsMap.entrySet()) {
                MultiSet<String>[] arg_sets = entry.getValue();
                List<String>[] arg_const_lists = new List[arg_sets.length];
                for (int i = 0; i < arg_sets.length; i++) {
                    arg_const_lists[i] = arg_sets[i].elementsAboveProportion(MIN_CONSTANT_PROPORTION);
                }
                functor2PromisingConstMap.put(entry.getKey(), arg_const_lists);
            }

            /* 添加所有的functor到队列 */
            if (debug) {
                Set<String> black_list = new HashSet<>();
//                black_list.add("sibling");
//                black_list.addAll(functor2ArityMap.keySet());

                Set<String> white_list = new HashSet<>();
//                white_list.add("gender");

                for (String functor: functor2ArityMap.keySet()) {
                    if (white_list.contains(functor) || !black_list.contains(functor)) {
                        waitingHeadFunctors.add(functor);
                    }
                }
            } else {
                waitingHeadFunctors.addAll(curFunctor2ArgSetsMap.keySet());
            }

            int total_facts = kb.totalFacts();
            System.out.printf(
                    "BK loaded: %d predicates; %d constants, %d facts\n",
                    functor2ArityMap.size(), kb.totalConstants(), total_facts
            );
            return total_facts;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    protected boolean shouldContinue() {
        return shouldContinue;
    }

    @Override
    protected Rule findRule() {
        /* 逐个functor找rule */
        do {
            int last_idx = waitingHeadFunctors.size() - 1;
            String functor = waitingHeadFunctors.get(last_idx);
            Integer arity = functor2ArityMap.get(functor);
            Rule rule = findRuleHandler(new Rule(functor, arity));
            if (null != rule && rule.getEval().useful(evalType)) {
                return rule;
            } else {
                waitingHeadFunctors.remove(last_idx);
            }
        }
        while (!waitingHeadFunctors.isEmpty());
        return null;
    }

    protected Rule findRuleHandler(Rule startRule) {
        /* 初始化Evaluation Cache */
        Map<Rule, Eval> eval_cache = new HashMap<>();
        evalRule(startRule, eval_cache);

        /* 初始化beams */
        Set<Rule> beams = new HashSet<>();
        beams.add(startRule);
        PriorityQueue<Rule> optimals = new PriorityQueue<>(
                Comparator.comparingDouble((Rule r) -> r.getEval().value(evalType)).reversed()
        );

        /* 寻找局部最优（只要进入这个循环，一定有局部最优） */
        while (true) {
            /* 根据当前beam遍历下一轮的所有candidates */
            PriorityQueue<Rule> candidates = new PriorityQueue<>(
                    Comparator.comparingDouble((Rule r) -> r.getEval().value(evalType)).reversed()
            );
            for (Rule r: beams) {
                System.out.printf("Extend: %s\n", r);
                candidates.add(r);
                Rule r_max = r;

                /* 遍历r的扩展邻居 */
                List<Rule> extensions = findExtension(r, eval_cache);
                for (Rule r_e : extensions) {
                    if (r_e.getEval().value(evalType) > r.getEval().value(evalType)) {
                        candidates.add(r_e);
                        if (r_e.getEval().value(evalType) > r_max.getEval().value(evalType)) {
                            r_max = r_e;
                        }
                    }
                }

                /* 遍历r的前驱邻居 */
                List<Rule> origins = findOrigin(r, eval_cache);
                for (Rule r_o : origins) {
                    if (r_o.getEval().value(evalType) > r.getEval().value(evalType)) {
                        candidates.add(r_o);
                        if (r_o.getEval().value(evalType) > r_max.getEval().value(evalType)) {
                            r_max = r_o;
                        }
                    }
                }

                if (r_max == r) {
                    optimals.add(r);
                }
            }

            /* 如果有多个optimal，选择最优的返回 */
            if (!optimals.isEmpty()) {
                return optimals.peek();
            }

            /* 找出下一轮的beams，同时检查optimal */
            Set<Rule> new_beams = new HashSet<>();
            Rule beam_rule;
            while (new_beams.size() < beamWidth && (null != (beam_rule = candidates.poll()))) {
                new_beams.add(beam_rule);
            }
            beams = new_beams;
        }
    }

    protected void evalRule(Rule rule, Map<Rule, Eval> evalCache) {
        Eval cache = evalCache.get(rule);
        if (null != cache) {
            rule.setEval(cache);
            return;
        }

        try {
            kb.evalRule(rule);
        } catch (SQLException e) {
            e.printStackTrace();
            rule.setEval(Eval.MIN);
        }
        evalCache.put(rule, rule.getEval());
    }

    protected List<Rule> findExtension(
            Rule rule, Map<Rule, Eval> evalCache
    ) {
        List<Rule> extensions = new ArrayList<>();

        /* 先找到所有空白的参数 */
        List<int[]> vacant_list = new ArrayList<>();    // 空白参数记录表：{pred_idx, arg_idx}
        for (int pred_idx = 0; pred_idx < rule.length(); pred_idx++) {
            Predicate pred_info = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < pred_info.arity(); arg_idx++) {
                if (null == pred_info.args[arg_idx]) {
                    vacant_list.add(new int[]{pred_idx, arg_idx});
                }
            }
        }

        /* 尝试增加已知变量 */
        for (int var_id = 0; var_id < rule.usedBoundedVars(); var_id++) {
            for (int[] vacant: vacant_list) {
                /* 尝试将已知变量填入空白参数 */
                Rule new_rule = new Rule(rule);
                new_rule.boundFreeVar2ExistedVar(vacant[0], vacant[1], var_id);
                checkThenAddRule(extensions, new_rule, evalCache);
            }

            for (Map.Entry<String, Integer> entry: functor2ArityMap.entrySet()) {
                /* 拓展一个谓词，并尝试一个已知变量 */
                String functor = entry.getKey();
                int arity = entry.getValue();
                int new_pred_idx = rule.length();
                Rule new_rule_template = new Rule(rule);
                new_rule_template.addPred(functor, arity);
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    Rule new_rule = new Rule(new_rule_template);
                    new_rule.boundFreeVar2ExistedVar(new_pred_idx, arg_idx, var_id);
                    checkThenAddRule(extensions, new_rule, evalCache);
                }
            }
        }

        for (int i = 0; i < vacant_list.size(); i++) {
            /* 找到新变量的第一个位置 */
            int[] first_vacant = vacant_list.get(i);

            /* 拓展一个常量 */
            Predicate predicate = rule.getPredicate(first_vacant[0]);
            List<String> const_list = functor2PromisingConstMap.get(predicate.functor)[first_vacant[1]];
            for (String const_symbol: const_list) {
                Rule new_rule = new Rule(rule);
                new_rule.boundFreeVar2Constant(first_vacant[0], first_vacant[1], DEFAULT_CONST_ID, const_symbol);
                checkThenAddRule(extensions, new_rule, evalCache);
            }

            /* 找到两个位置尝试同一个新变量 */
            for (int j = i + 1; j < vacant_list.size(); j++) {
                /* 新变量的第二个位置可以是当前rule中的其他空位 */
                int[] second_vacant = vacant_list.get(j);
                Rule new_rule_info = new Rule(rule);
                new_rule_info.boundFreeVars2NewVar(
                        first_vacant[0], first_vacant[1], second_vacant[0], second_vacant[1]
                );
                checkThenAddRule(extensions, new_rule_info, evalCache);
            }
            for (Map.Entry<String, Integer> entry: functor2ArityMap.entrySet()) {
                /* 新变量的第二个位置也可以是拓展一个谓词以后的位置 */
                String functor = entry.getKey();
                int arity = entry.getValue();
                int new_pred_idx = rule.length();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    Rule new_rule_info = new Rule(rule);
                    new_rule_info.addPred(functor, arity);
                    new_rule_info.boundFreeVars2NewVar(
                            first_vacant[0], first_vacant[1], new_pred_idx, arg_idx
                    );
                    checkThenAddRule(extensions, new_rule_info, evalCache);
                }
            }
        }

        return extensions;
    }

    protected List<Rule> findOrigin(Rule rule, Map<Rule, Eval> evalCache) {
        List<Rule> origins = new ArrayList<>();
        for (int pred_idx = 0; pred_idx < rule.length(); pred_idx++) {
            /* 从Head开始删除可能会出现Head中没有Bounded Var但是Body不为空的情况，按照定义来说，这种规则是不在
               搜索空间中的，但是会被isInvalid方法检查出来 */
            Predicate predicate = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (null != predicate.args[arg_idx]) {
                    Rule new_rule = new Rule(rule);
                    new_rule.removeKnownArg(pred_idx, arg_idx);
                    checkThenAddRule(origins, new_rule, evalCache);
                }
            }
        }

        return origins;
    }

    protected void checkThenAddRule(Collection<Rule> collection, Rule rule, Map<Rule, Eval> evalCache) {
        if (!rule.isInvalid()) {
//            System.out.printf("\tEvaluating: %s\n", rule);
            evalRule(rule, evalCache);
            collection.add(rule);
        }
    }

    @Override
    protected void updateKb(Rule rule) {
        if (null == rule) {
            shouldContinue = false;
            showHypothesis();
            return;
        }

        hypothesis.add(rule);
//        showHypothesis();
        System.out.println("ADD: " + rule);

        try {
            List<Predicate[]> groundings = kb.findGroundings(rule);
            kb.addNewProofs(groundings);
            counterExamples.addAll(kb.findCounterExamples(rule));

            /* 把rule grounding在图中表达出来 */
            for (Predicate[] grounding: groundings) {
                Predicate head = grounding[0];
                BaseGraphNode<Predicate> head_node = predicate2NodeMap.computeIfAbsent(
                        head, k -> new BaseGraphNode<>(head)
                );
                graph.computeIfAbsent(head_node, k -> {
                    Set<BaseGraphNode<Predicate>> neighbours = new HashSet<>();
                    for (int i = 1; i < grounding.length; i++) {
                        Predicate body = grounding[i];
                        BaseGraphNode<Predicate> body_node = predicate2NodeMap.computeIfAbsent(
                                body, kk -> new BaseGraphNode<>(body)
                        );
                        neighbours.add(body_node);
                    }
                    return neighbours;
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void findStartSet() {
        /* 在更新KB的时候已经把Graph顺便做好了，这里只需要查找对应的点即可 */
        /* 找出所有不能被prove的点 */
        try {
            startSet = kb.findUnprovedPredicates();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Error(e);
        }

        /* 找出所有SCC中的覆盖点 */
        final int start_set_size_without_scc = startSet.size();
        int scc_total_vertices = 0;
        int fvs_total_vertices = 0;
        Tarjan<BaseGraphNode<Predicate>> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode<Predicate>>> sccs = tarjan.run();
        for (Set<BaseGraphNode<Predicate>> scc: sccs) {
            /* 找出FVS的一个解，并把之放入start_set */
            FeedbackVertexSetSolver<BaseGraphNode<Predicate>> fvs_solver = new FeedbackVertexSetSolver<>(graph, scc);
            Set<BaseGraphNode<Predicate>> fvs = fvs_solver.run();
            for (BaseGraphNode<Predicate> node: fvs) {
                startSet.add(node.content);
            }
            scc_total_vertices += scc.size();
            fvs_total_vertices += fvs.size();
        }

        System.out.println("- Core Statistics:");
        System.out.println("---");
        System.out.printf("# %10s %10s %10s %10s %10s\n", "|N|", "|N-SCC|", "#SCC", "|SCC|", "FVS");
        System.out.printf("# %10d %10d %10d %10d %10d\n",
                startSet.size(),
                start_set_size_without_scc,
                sccs.size(),
                scc_total_vertices,
                fvs_total_vertices
        );
        System.out.println("---");
    }

    @Override
    protected void findCounterExamples() {
        /* Counter Example 已经在更新KB的时候找出来了，这里什么也不做 */
        System.out.printf("Counter Examples Found: %d in COUNTER EXAMPLE set\n", counterExamples.size());
    }

    private void showHypothesis() {
        System.out.println("\nHypothesis Found:");
        for (Rule rule: hypothesis) {
            System.out.println(rule);
        }
    }

    @Override
    public List<Rule> dumpHypothesis() {
        return hypothesis;
    }

    @Override
    public Set<Predicate> dumpStartSet() {
        return startSet;
    }

    @Override
    public Set<Predicate> dumpCounterExampleSet() {
        return counterExamples;
    }

    @Override
    protected Iterator<Predicate> originalBkIterator() {
        return kb.originalFactIterator();
    }

    @Override
    protected Map<BaseGraphNode<Predicate>, Set<BaseGraphNode<Predicate>>> getDependencyGraph() {
        return graph;
    }

    @Override
    public Compound fact2Compound(Predicate fact) {
        Term[] args = new Term[fact.arity()];
        for (int arg_idx = 0; arg_idx < args.length; arg_idx++) {
            args[arg_idx] = new Atom(fact.args[arg_idx].name);
        }
        return new Compound(fact.functor, args);
    }

    public static void main(String[] args) throws IOException {
        SincBasicWithRKB compressor = new SincBasicWithRKB(
                1,
                3,
                EvalMetric.CompressionCapacity,
//                EvalMetric.CompressionRate,
//                EvalMetric.InfoGain,
//                "testData/familyRelation/FamilyRelationSimple(0.00)(10x).tsv",
//                "testData/familyRelation/FamilyRelationMedium(0.00)(10x).tsv",
//                "testData/RKB/Elti.tsv",
//                "testData/RKB/Dunur.tsv",
//                "testData/RKB/StudentLoan.tsv",
//                "testData/RKB/dbpedia_factbook.tsv",
//                "testData/RKB/dbpedia_lobidorg.tsv",
                "testData/RKB/webkb.cornell.tsv",
//                "testData/RKB/webkb.texas.tsv",
//                "testData/RKB/webkb.washington.tsv",
//                "testData/RKB/webkb.wisconsin.tsv",
                false
        );
        compressor.run();
//        List<Rule> rules = compressor.dumpHypothesis();
//        Set<Compound> start_set = compressor.dumpStartSet();
//
//        PrintWriter writer = new PrintWriter("test.pl");
//        for (Compound fact: start_set) {
//            writer.println(fact);
//        }
//        for (Rule rule: rules) {
//            writer.println(rule.toCompleteRuleString());
//        }
//        writer.close();

//        try {
//            if (compressor.validate()) {
//                System.out.println("Validation Passed!");
//            } else {
//                System.err.println("[ERROR]Validation Failed!\n");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.err.println("[ERROR]Validation Failed with Exception!\n");
//        }
    }

}
