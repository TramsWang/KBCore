package sinc;

import sinc.common.*;
import sinc.util.graph.BaseGraphNode;
import sinc.util.graph.FeedbackVertexSetSolver;
import sinc.util.graph.Tarjan;

import java.util.*;

public abstract class SInC {

    protected static final int CONST_ID = -1;
    protected static final BaseGraphNode<Predicate> AXIOM_NODE = new BaseGraphNode<>(new Predicate("⊥", 0));

    protected final SincConfig config;
    protected final String kbPath;
    protected final String dumpPath;

    private final List<Rule> hypothesis = new ArrayList<>();
    private final Map<Predicate, BaseGraphNode<Predicate>> predicate2NodeMap = new HashMap<>();
    private final Map<BaseGraphNode<Predicate>, Set<BaseGraphNode<Predicate>>> dependencyGraph = new HashMap<>();
    private final Set<Predicate> startSet = new HashSet<>();
    private final Set<Predicate> counterExamples = new HashSet<>();
    private final PerformanceMonitor performanceMonitor = new PerformanceMonitor();

    private static class GraphAnalyseResult {
        public int startSetSize = 0;
        public int startSetSizeWithoutFvs = 0;
        public int sccNumber = 0;
        public int sccVertices = 0;
        public int fvsVertices = 0;
    }

    public SInC(SincConfig config, String kbPath, String dumpPath) {
        this.config = config;
        this.kbPath = kbPath;
        this.dumpPath = dumpPath;
        Rule.MIN_HEAD_COVERAGE = config.minHeadCoverage;
    }

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     *
     * Assume no duplication.
     *
     * @return 原KB的fact数量，|B|
     */
    abstract protected int loadKb();

    abstract protected List<String> getTargetFunctors();

    abstract protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache);

    protected Rule findRule(String headFunctor) {
        final Set<RuleFingerPrint> cache = new HashSet<>();
        final Rule start_rule = getStartRule(headFunctor, cache);

        /* 初始化beams */
        final Eval.EvalMetric eval_metric = config.evalMetric;
        final int beam_width = config.beamWidth;
        Set<Rule> beams = new HashSet<>();
        beams.add(start_rule);
        PriorityQueue<Rule> optimals = new PriorityQueue<>(
                Comparator.comparingDouble((Rule r) -> r.getEval().value(eval_metric)).reversed()
        );

        /* 寻找局部最优（只要进入这个循环，一定有局部最优） */
        while (true) {
            /* 根据当前beam遍历下一轮的所有candidates */
            PriorityQueue<Rule> candidates = new PriorityQueue<>(
                    Comparator.comparingDouble((Rule r) -> r.getEval().value(eval_metric)).reversed()
            );
            for (Rule r: beams) {
                System.out.printf("Extend: %s\n", r);
                candidates.add(r);
                Rule r_max = r;

                /* 遍历r的邻居 */
                final List<Rule> extensions = findExtension(r);
                final List<Rule> origins;
                if (config.searchOrigins) {
                    origins = findOrigin(r);
                } else {
                    origins = new ArrayList<>();
                }
                for (Rule r_e : extensions) {
                    if (r_e.getEval().value(eval_metric) > r.getEval().value(eval_metric)) {
                        candidates.add(r_e);
                        if (r_e.getEval().value(eval_metric) > r_max.getEval().value(eval_metric)) {
                            r_max = r_e;
                        }
                    }
                }
                if (config.searchOrigins) {
                    for (Rule r_o : origins) {
                        if (r_o.getEval().value(eval_metric) > r.getEval().value(eval_metric)) {
                            candidates.add(r_o);
                            if (r_o.getEval().value(eval_metric) > r_max.getEval().value(eval_metric)) {
                                r_max = r_o;
                            }
                        }
                    }
                }

                if (r_max == r) {
                    optimals.add(r);
                }

                /* 监测：分支数量信息 */
                performanceMonitor.branchProgress.add(new PerformanceMonitor.BranchInfo(
                        r.size(), extensions.size(), origins.size()
                ));
            }

            /* 如果有多个optimal，选择最优的返回 */
            if (!optimals.isEmpty()) {
                return optimals.peek();
            }

            /* 找出下一轮的beams，同时检查optimal */
            Set<Rule> new_beams = new HashSet<>();
            Rule beam_rule;
            while (new_beams.size() < beam_width && (null != (beam_rule = candidates.poll()))) {
                new_beams.add(beam_rule);
            }
            beams = new_beams;
        }
    }

    protected List<Rule> findExtension(final Rule rule) {
        List<Rule> extensions = new ArrayList<>();

        /* 先找到所有空白的参数 */
        class ArgPos {
            public final int predIdx;
            public final int argIdx;

            public ArgPos(int predIdx, int argIdx) {
                this.predIdx = predIdx;
                this.argIdx = argIdx;
            }
        }
        List<ArgPos> vacant_list = new ArrayList<>();    // 空白参数记录表：{pred_idx, arg_idx}
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.length(); pred_idx++) {
            final Predicate pred_info = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < pred_info.arity(); arg_idx++) {
                if (null == pred_info.args[arg_idx]) {
                    vacant_list.add(new ArgPos(pred_idx, arg_idx));
                }
            }
        }

        /* 尝试增加已知变量 */
        final Map<String, Integer> func_2_arity_map = getFunctor2ArityMap();
        for (int var_id = 0; var_id < rule.usedBoundedVars(); var_id++) {
            for (ArgPos vacant: vacant_list) {
                /* 尝试将已知变量填入空白参数 */
                final Rule new_rule = rule.clone();
                if (new_rule.boundFreeVar2ExistingVar(vacant.predIdx, vacant.argIdx, var_id)) {
                    extensions.add(new_rule);
                }
            }

            for (Map.Entry<String, Integer> entry: func_2_arity_map.entrySet()) {
                /* 拓展一个谓词，并尝试一个已知变量 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    final Rule new_rule = rule.clone();
                    if (new_rule.boundFreeVar2ExistingVar(functor, arity, arg_idx, var_id)) {
                        extensions.add(new_rule);
                    }
                }
            }
        }

        final Map<String, List<String>[]> func_2_promising_const_map = getFunctor2PromisingConstantMap();
        for (int i = 0; i < vacant_list.size(); i++) {
            /* 找到新变量的第一个位置 */
            final ArgPos first_vacant = vacant_list.get(i);

            /* 拓展一个常量 */
            final Predicate predicate = rule.getPredicate(first_vacant.predIdx);
            final List<String> const_list = func_2_promising_const_map.get(predicate.functor)[first_vacant.argIdx];
            for (String const_symbol: const_list) {
                final Rule new_rule = rule.clone();
                if (new_rule.boundFreeVar2Constant(first_vacant.predIdx, first_vacant.argIdx, const_symbol)) {
                    extensions.add(new_rule);
                }
            }

            /* 找到两个位置尝试同一个新变量 */
            for (int j = i + 1; j < vacant_list.size(); j++) {
                /* 新变量的第二个位置可以是当前rule中的其他空位 */
                final ArgPos second_vacant = vacant_list.get(j);
                final Rule new_rule = rule.clone();
                if (new_rule.boundFreeVars2NewVar(
                        first_vacant.predIdx, first_vacant.argIdx, second_vacant.predIdx, second_vacant.argIdx
                )) {
                    extensions.add(new_rule);
                }
            }
            for (Map.Entry<String, Integer> entry: func_2_arity_map.entrySet()) {
                /* 新变量的第二个位置也可以是拓展一个谓词以后的位置 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    final Rule new_rule = rule.clone();
                    if (new_rule.boundFreeVars2NewVar(
                            functor, arity, arg_idx, first_vacant.predIdx, first_vacant.argIdx
                    )) {
                        extensions.add(new_rule);
                    }
                }
            }
        }

        return extensions;
    }

    abstract protected Map<String, Integer> getFunctor2ArityMap();

    abstract protected Map<String, List<String>[]> getFunctor2PromisingConstantMap();

    protected List<Rule> findOrigin(Rule rule) {
        final List<Rule> origins = new ArrayList<>();
        for (int pred_idx = 0; pred_idx < rule.length(); pred_idx++) {
            /* 从Head开始删除可能会出现Head中没有Bounded Var但是Body不为空的情况，按照定义来说，这种规则是不在
               搜索空间中的，但是会被isInvalid方法检查出来 */
            final Predicate predicate = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (null != predicate.args[arg_idx]) {
                    final Rule new_rule = rule.clone();
                    if (new_rule.removeBoundedArg(pred_idx, arg_idx)) {
                        origins.add(new_rule);
                    }
                }
            }
        }

        return origins;
    }

    abstract protected UpdateResult updateKb(Rule rule);

    protected void updateGraph(List<Predicate[]> groundings) {
        for (Predicate[] grounding: groundings) {
            final Predicate head_pred = grounding[Rule.HEAD_PRED_IDX];
            BaseGraphNode<Predicate> head_node = predicate2NodeMap.computeIfAbsent(
                    head_pred, k -> new BaseGraphNode<>(head_pred)
            );
            dependencyGraph.compute(head_node, (h, dependencies) -> {
                if (null == dependencies) {
                    dependencies = new HashSet<>();
                }
                if (1 >= grounding.length) {
                    /* dependency为公理 */
                    dependencies.add(AXIOM_NODE);
                } else {
                    for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < grounding.length; pred_idx++) {
                        final Predicate body_pred = grounding[pred_idx];
                        final BaseGraphNode<Predicate> body_node = predicate2NodeMap.computeIfAbsent(
                                body_pred, kk -> new BaseGraphNode<>(body_pred)
                        );
                        dependencies.add(body_node);
                    }
                }
                return dependencies;
            });
        }
    }

    protected GraphAnalyseResult findStartSet() {
        /* 在更新KB的时候已经把Graph顺便做好了，这里只需要查找对应的点即可 */
        /* 找出所有不能被prove的点 */
        final GraphAnalyseResult result = new GraphAnalyseResult();
        for (Predicate fact : getOriginalKb()) {
            final BaseGraphNode<Predicate> fact_node = new BaseGraphNode<>(fact);
            if (!dependencyGraph.containsKey(fact_node)) {
                startSet.add(fact);
            }
        }

        /* 找出所有SCC中的覆盖点 */
        result.startSetSizeWithoutFvs = startSet.size();
        final Tarjan<BaseGraphNode<Predicate>> tarjan = new Tarjan<>(dependencyGraph);
        final List<Set<BaseGraphNode<Predicate>>> sccs = tarjan.run();
        result.sccNumber = sccs.size();

        for (Set<BaseGraphNode<Predicate>> scc: sccs) {
            /* 找出FVS的一个解，并把之放入start_set */
            final FeedbackVertexSetSolver<BaseGraphNode<Predicate>> fvs_solver =
                    new FeedbackVertexSetSolver<>(dependencyGraph, scc);
            final Set<BaseGraphNode<Predicate>> fvs = fvs_solver.run();
            for (BaseGraphNode<Predicate> node: fvs) {
                startSet.add(node.content);
            }
            result.sccVertices += scc.size();
            result.fvsVertices += fvs.size();
        }

        result.startSetSize = startSet.size();
        return result;
    }

    protected boolean validate() {
        /* Todo: Implement Here */
        return true;
    }

    protected void dumpResult() {
        /* Todo: Implement Here */
    }

    abstract protected Set<Predicate> getOriginalKb();

    public final void run() {
        /* 加载KB */
        final long time_start = System.currentTimeMillis();
        performanceMonitor.kbSize = loadKb();
        final long time_kb_loaded = System.currentTimeMillis();
        performanceMonitor.kbLoadTime = time_kb_loaded - time_start;

        /* 逐个functor找rule */
        long time_rule_finding_start = time_kb_loaded;
        final List<String> target_head_functors = getTargetFunctors();
        do {
            int last_idx = target_head_functors.size() - 1;
            String functor = target_head_functors.get(last_idx);
            Rule rule = findRule(functor);
            final long time_rule_found = System.currentTimeMillis();
            performanceMonitor.hypothesisMiningTime += time_rule_found - time_rule_finding_start;

            if (null != rule) {
                hypothesis.add(rule);
                performanceMonitor.hypothesisSize += rule.size();

                /* 更新grpah和counter example */
                UpdateResult update_result = updateKb(rule);
                counterExamples.addAll(update_result.counterExamples);
                updateGraph(update_result.groundings);
                final long time_kb_updated = System.currentTimeMillis();
                performanceMonitor.otherMiningTime += time_kb_updated - time_rule_found;
            } else {
                target_head_functors.remove(last_idx);
            }
            time_rule_finding_start = System.currentTimeMillis();
        } while (!target_head_functors.isEmpty());
        performanceMonitor.hypothesisRuleNumber = hypothesis.size();
        performanceMonitor.counterExampleSize = counterExamples.size();

        /* 解析Graph找start set */
        GraphAnalyseResult graph_analyse_result = findStartSet();
        performanceMonitor.startSetSize = graph_analyse_result.startSetSize;
        performanceMonitor.startSetSizeWithoutFvs = graph_analyse_result.startSetSizeWithoutFvs;
        performanceMonitor.sccNumber = graph_analyse_result.sccNumber;
        performanceMonitor.sccVertices = graph_analyse_result.sccVertices;
        performanceMonitor.fvsVertices = graph_analyse_result.fvsVertices;
        final long time_start_set_found = System.currentTimeMillis();
        performanceMonitor.otherMiningTime += time_start_set_found - time_rule_finding_start;

        /* 检查结果 */
        if (config.validation) {
            if (!validate()) {
                System.err.println("[ERROR] Validation Failed");
            }
        }
        final long time_validation_done = System.currentTimeMillis();
        performanceMonitor.validationTime = time_validation_done - time_start_set_found;

        /* 记录结果 */
        dumpResult();
        final long time_dumped = System.currentTimeMillis();
        performanceMonitor.dumpTime = time_dumped - time_validation_done;
        performanceMonitor.totalTime = time_dumped - time_start;

        performanceMonitor.show();

        if (config.debug) {
            /* Todo: 图结构上传Neo4j */
            System.out.println("[DEBUG] Upload Graph to Neo4J...");
        }
    }
}
