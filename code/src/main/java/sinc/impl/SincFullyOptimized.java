package sinc.impl;

import org.jpl7.*;
import org.jpl7.Variable;
import sinc.SInC;
import sinc.common.*;
import sinc.util.MultiSet;
import sinc.util.PrologModule;
import sinc.util.SwiplUtil;
import sinc.util.graph.FeedbackVertexSetSolver;
import sinc.util.graph.Tarjan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.*;

public class SincFullyOptimized extends SInC {

    protected static final double MIN_HEAD_COVERAGE = 0.05;
    protected static final double MIN_CONSTANT_PROPORTION = 0.25;
    protected static final int DEFAULT_CONST_ID = -1;
    protected static final Compound AXIOM = new Compound("⊥", new Term[0]);

    protected final Map<String, Set<Compound>> globalFunctor2FactSetMap = new HashMap<>();
    protected final Map<String, Set<Compound>> curFunctor2FactSetMap = new HashMap<>();
    protected final Map<String, Integer> functor2ArityMap = new HashMap<>();
    protected final Map<String, MultiSet<String>[]> curFunctor2ArgSetsMap = new HashMap<>();
    protected final Set<String> constants = new HashSet<>();
    protected final Map<String, List<String>[]> functor2PromisingConstMap = new HashMap<>();
    protected final List<Rule> hypothesis = new ArrayList<>();
    protected boolean shouldContinue = true;
    protected List<String> waitingHeadFunctors = new ArrayList<>();

    protected final Map<Compound, GraphNode4Compound> compound2NodeMap = new HashMap<>();
    protected final Map<GraphNode4Compound, Set<GraphNode4Compound>> graph = new HashMap<>();
    protected final Set<Compound> counterExamples = new HashSet<>();
    protected final Set<Compound> startSet = new HashSet<>();

    public SincFullyOptimized(
            EvalMetric evalType,
            String kbFilePath, String hypothesisFilePath, String startSetFilePath, String counterExampleSetFilePath,
            boolean debug
    ) {
        super(evalType, kbFilePath, hypothesisFilePath, startSetFilePath, counterExampleSetFilePath, debug);
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
                MultiSet<String>[] arg_set_list =  curFunctor2ArgSetsMap.computeIfAbsent(functor, k -> {
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
                    constants.add(components[i]);
                }
                Compound compound = new Compound(functor, args);
                SwiplUtil.appendKnowledge(PrologModule.GLOBAL, compound);
                globalFunctor2FactSetMap.compute(functor, (k, v) -> {
                    if (null == v) {
                        v = new HashSet<>();
                    }
                    v.add(compound);
                    return v;
                });
                curFunctor2FactSetMap.compute(functor, (k, v) -> {
                    if (null == v) {
                        v = new HashSet<>();
                    }
                    v.add(compound);
                    return v;
                });
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
                black_list.addAll(functor2ArityMap.keySet());

                Set<String> white_list = new HashSet<>();
                white_list.add("gender");

                for (String functor: functor2ArityMap.keySet()) {
                    if (white_list.contains(functor) || !black_list.contains(functor)) {
                        waitingHeadFunctors.add(functor);
                    }
                }
            } else {
                waitingHeadFunctors.addAll(curFunctor2ArgSetsMap.keySet());
            }

            int total_facts = 0;
            for (Set<Compound> fact_set: globalFunctor2FactSetMap.values()) {
                total_facts += fact_set.size();
            }
            System.out.printf(
                    "BK loaded: %d predicates; %d constants, %d facts\n",
                    curFunctor2FactSetMap.size(), constants.size(), total_facts
            );
            return total_facts;
        } catch (IOException e) {
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

        /* 寻找局部最优（只要进入这个循环，一定有局部最优） */
        Rule r_max = startRule;
        evalRule(r_max, eval_cache);
        while (true) {
            System.out.printf("Extend: %s\n", r_max);
            Rule r_e_max = r_max;

            /* 遍历r_max的扩展邻居 */
            List<Rule> extensions = findExtension(r_max, eval_cache);
            for (Rule r_e: extensions) {
                if (r_e.getEval().value(evalType) > r_e_max.getEval().value(evalType)) {
                    r_e_max = r_e;
                }
            }

            /* 遍历r_max的前驱邻居 */
            List<Rule> origins = findOrigin(r_max, eval_cache);
            for (Rule r_o: origins) {
                if (r_o.getEval().value(evalType) > r_e_max.getEval().value(evalType)) {
                    r_e_max = r_o;
                }
            }

            if (r_e_max == r_max) {
                return r_max;
            }
            r_max = r_e_max;
        }
    }

    protected void evalRule(Rule rule, Map<Rule, Eval> evalCache) {
        Eval cache = evalCache.get(rule);
        if (null != cache) {
            rule.setEval(cache);
            return;
        }

        /* 统计Head的参数情况，并将其转成带Free Var的Jpl Arg Array */
        Predicate head_pred = rule.getHead();
        List<String> bounded_vars_in_head = new ArrayList<>();
        Term[] head_args = new Term[head_pred.args.length];
        int free_var_cnt_in_head = 0;
        for (int arg_idx = 0; arg_idx < head_args.length; arg_idx++) {
            Argument argument = head_pred.args[arg_idx];
            if (null == argument) {
                head_args[arg_idx] = new Variable(String.format("Y%d", free_var_cnt_in_head));
                free_var_cnt_in_head++;
            } else if (argument.isVar) {
                head_args[arg_idx] = new Variable(argument.name);
                bounded_vars_in_head.add(argument.name);
            } else {
                head_args[arg_idx] = new Atom(argument.name);
            }
        }
        Set<Compound> global_facts = globalFunctor2FactSetMap.get(head_pred.functor);
        Set<Compound> cur_facts = curFunctor2FactSetMap.get(head_pred.functor);

        /* 如果head上的所有变量都是自由变量则直接计算 */
        if (head_pred.arity() == free_var_cnt_in_head) {
            rule.setEval(
                    new Eval(
                            cur_facts.size(),
                            Math.pow(constants.size(), head_pred.arity()) -
                                    global_facts.size() + cur_facts.size(),
                            rule.size()
                    )
            );
            evalCache.put(rule, rule.getEval());
            return;
        }

        /* 计算all entailments */
        Set<String> bounded_vars_in_body = new HashSet<>();
        StringBuilder query_builder = new StringBuilder();
        if (2 <= rule.length()) {
            for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
                Predicate body_pred = rule.getPredicate(pred_idx);
                Term[] args = new Term[body_pred.arity()];
                for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                    Argument argument = body_pred.args[arg_idx];
                    if (null == argument) {
                        args[arg_idx] = new Variable("_");
                    } else if (argument.isVar) {
                        args[arg_idx] = new Variable(argument.name);
                        bounded_vars_in_body.add(argument.name);
                    } else {
                        args[arg_idx] = new Atom(argument.name);
                    }
                }
                Compound body_compound = new Compound(body_pred.functor, args);
                query_builder.append(body_compound.toString()).append(',');
            }
            query_builder.deleteCharAt(query_builder.length() - 1);
        }
        String query_str = query_builder.toString();
        Set<Compound> head_templates = new HashSet<>();
        boolean body_is_not_empty = !"".equals(query_str);
        if (body_is_not_empty) {
            Query q = new Query(":", new Term[]{
                    new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(query_str)
            });
            for (Map<String, Term> binding : q) {
                Term[] template_args = new Term[bounded_vars_in_head.size()];
                for (int arg_idx = 0; arg_idx < template_args.length; arg_idx++) {
                    template_args[arg_idx] = binding.get(bounded_vars_in_head.get(arg_idx));
                }
                head_templates.add(new Compound("h", template_args));
            }
            q.close();
        }
        Set<String> bounded_vars_in_head_only = new HashSet<>();
        for (String bv_head : bounded_vars_in_head) {
            if (!bounded_vars_in_body.contains(bv_head)) {
                /* 找出所有仅在Head中出现的bounded var */
                bounded_vars_in_head_only.add(bv_head);
            }
        }
        final double all_entailments = (body_is_not_empty ? head_templates.size() : 1) * Math.pow(
                constants.size(), free_var_cnt_in_head + bounded_vars_in_head_only.size()
        );

        /* 计算positive entailments */
        Compound head_compound = new Compound(head_pred.functor, head_args);
        query_str = body_is_not_empty ? head_compound.toString() + ',' + query_str :
                head_compound.toString();

        Query q = new Query(":", new Term[]{
                new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(query_str)
        });
        Set<Compound> head_instances = new HashSet<>();
        for (Map<String, Term> binding: q) {
            head_instances.add(SwiplUtil.substitute(head_compound, binding));
        }
        q.close();

        int positive_entailments = 0;
        int already_entailed = 0;
        for (Compound head_instance: head_instances) {
            if (cur_facts.contains(head_instance)) {
                positive_entailments++;
            } else if (global_facts.contains(head_instance)) {
                already_entailed++;
            }
        }

        /* 用HC剪枝 */
        double head_coverage = ((double) positive_entailments) / global_facts.size();
        if (MIN_HEAD_COVERAGE >= head_coverage) {
            rule.setEval(Eval.MIN);
            evalCache.put(rule, rule.getEval());
            return;
        }

        rule.setEval(new Eval(positive_entailments, all_entailments - already_entailed, rule.size()));
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
            evalRule(rule, evalCache);
            collection.add(rule);
        }
    }

    @Override
    protected void updateKb(Rule rule) {
        if (null == rule) {
            shouldContinue = false;
            return;
        }

        hypothesis.add(rule);
        showHypothesis();

        /* 找出所有的entailment */
        int removed_cnt = 0;

        /* 统计Head的参数情况，并将其转成带Free Var的Jpl Compound */
        Predicate head_pred = rule.getHead();
        List<String> bounded_vars_in_head = new ArrayList<>();
        Term[] head_args = new Term[head_pred.args.length];
        int free_var_cnt_in_head = 0;
        for (int arg_idx = 0; arg_idx < head_args.length; arg_idx++) {
            Argument argument = head_pred.args[arg_idx];
            if (null == argument) {
                head_args[arg_idx] = new Variable(String.format("Y%d", free_var_cnt_in_head));
                free_var_cnt_in_head++;
            } else if (argument.isVar) {
                head_args[arg_idx] = new Variable(argument.name);
                bounded_vars_in_head.add(argument.name);
            } else {
                head_args[arg_idx] = new Atom(argument.name);
            }
        }
        Compound head_compound = new Compound(head_pred.functor, head_args);
        Set<Compound> global_facts = globalFunctor2FactSetMap.get(head_pred.functor);
        Set<Compound> cur_fact_set = curFunctor2FactSetMap.get(head_pred.functor);

        /* 构造所有的dependency */
        boolean body_is_not_empty = (2 <= rule.length());
        if (body_is_not_empty) {
            /* 用body构造查询 */
            Set<String> bounded_vars_in_body = new HashSet<>();
            StringBuilder query_builder = new StringBuilder();
            List<Compound> body_compounds = new ArrayList<>(rule.length() - 1);
            for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
                Predicate body_pred = rule.getPredicate(pred_idx);
                Term[] args = new Term[body_pred.arity()];
                for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                    Argument argument = body_pred.args[arg_idx];
                    if (null == argument) {
                        args[arg_idx] = new Variable("_");
                    } else if (argument.isVar) {
                        args[arg_idx] = new Variable(argument.name);
                        bounded_vars_in_body.add(argument.name);
                    } else {
                        args[arg_idx] = new Atom(argument.name);
                    }
                }
                Compound body_compound = new Compound(body_pred.functor, args);
                body_compounds.add(body_compound);
                query_builder.append(body_compound.toString()).append(',');
            }
            query_builder.deleteCharAt(query_builder.length() - 1);
            String query_str = query_builder.toString();

            /* 找出所有仅在Head中出现的bounded var */
            Set<String> bounded_vars_in_head_only = new HashSet<>();
            for (String bv_head : bounded_vars_in_head) {
                if (!bounded_vars_in_body.contains(bv_head)) {
                    bounded_vars_in_head_only.add(bv_head);
                }
            }

            Query q_4_body_grounding = new Query(":", new Term[]{
                    new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(query_str)
            });
            for (Map<String, Term> binding : q_4_body_grounding) {
                /* 构造body grounding */
                List<Compound> body_groundings = new ArrayList<>(body_compounds.size());
                for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
                    Predicate body_pred = rule.getPredicate(pred_idx);
                    Compound body_compound = body_compounds.get(pred_idx - 1);
                    int free_vars_in_body = 0;
                    Term[] body_args = new Term[body_pred.arity()];
                    for (int arg_idx = 0; arg_idx < body_args.length; arg_idx++) {
                        Argument argument = body_pred.args[arg_idx];
                        if (null == argument) {
                            /* 对应位置的参数替换为带名字的free var */
                            body_args[arg_idx] = new Variable("Y" + free_vars_in_body);
                            free_vars_in_body++;
                        } else if (argument.isVar) {
                            /* 对应位置替换为binding的值 */
                            Term original_arg = body_compound.arg(arg_idx + 1);
                            body_args[arg_idx] = binding.getOrDefault(original_arg.name(), original_arg);
                        } else {
                            /* 对应位置保留常量不变 */
                            body_args[arg_idx] = body_compound.arg(arg_idx + 1);
                        }
                    }

                    Compound body_grounding = new Compound(body_pred.functor, body_args);
                    if (0 < free_vars_in_body) {
                        /* body中带有free var需要查找某一值替换 */
                        Query q_4_single_body_grounding = new Query(":", new Term[]{
                                new Atom(PrologModule.GLOBAL.getSessionName()), body_grounding
                        });
                        Map<String, Term> single_solution = q_4_single_body_grounding.nextSolution();
                        q_4_single_body_grounding.close();
                        body_grounding = SwiplUtil.substitute(body_grounding, single_solution);
                    }
                    body_groundings.add(body_grounding);
                }

                /* 构造head grounding*/
                Compound head_grounding = SwiplUtil.substitute(head_compound, binding);
                if (bounded_vars_in_head_only.isEmpty()) {
                    if (drawInGraph(cur_fact_set, global_facts, head_grounding, body_groundings)) {
                        removed_cnt++;
                    }
                } else {
                    /* 如果head中带有free var需要遍历所有可能值 */
                    Query q_4_head_grounding = new Query(":", new Term[]{
                            new Atom(PrologModule.GLOBAL.getSessionName()), head_grounding
                    });
                    for (Map<String, Term> head_binding: q_4_head_grounding) {
                        if (drawInGraph(
                                cur_fact_set, global_facts, SwiplUtil.substitute(
                                        head_grounding, head_binding
                                ), body_groundings
                        )) {
                            removed_cnt++;
                        }
                    }
                    q_4_head_grounding.close();
                }
            }
            q_4_body_grounding.close();
        } else {
            /* Body为True(i.e. AXIOM) */
            if (0 >= free_var_cnt_in_head) {
                if (drawInGraph(cur_fact_set, global_facts, head_compound, Collections.singletonList(AXIOM))) {
                    removed_cnt++;
                }
            } else {
                /* 如果head中带有free var需要遍历所有可能值 */
                Query q_4_head_grounding = new Query(":", new Term[]{
                        new Atom(PrologModule.GLOBAL.getSessionName()), head_compound
                });
                for (Map<String, Term> head_binding: q_4_head_grounding) {
                    if (drawInGraph(
                            cur_fact_set, global_facts, SwiplUtil.substitute(
                                    head_compound, head_binding
                            ), Collections.singletonList(AXIOM)
                    )) {
                        removed_cnt++;
                    }
                }
                q_4_head_grounding.close();
            }
        }
        System.out.printf("Update: %d removed\n", removed_cnt);
    }

    protected boolean drawInGraph(
            Set<Compound> curFactSet, Set<Compound> globalFactSet, Compound head, List<Compound> bodies
    ) {
        if (curFactSet.remove(head)) {
            /* 删除并在graph中加入dependency */
            GraphNode4Compound head_node = compound2NodeMap.computeIfAbsent(head, k -> new GraphNode4Compound(head));
            graph.compute(head_node, (k, v) -> {
                if (null == v) {
                    v = new HashSet<>();
                }
                for (Compound body: bodies) {
                    GraphNode4Compound body_node = compound2NodeMap.computeIfAbsent(body, kk -> new GraphNode4Compound(body));
                    v.add(body_node);
                }
                return v;
            });
            return true;
        } else if (!globalFactSet.contains(head)) {
            /* 加入反例集合 */
            counterExamples.add(head);
        }
        /* 否则就是已经被prove过的，忽略即可 */
        return false;
    }

    @Override
    protected void findStartSet() {
        /* 在更新KB的时候已经把Graph顺便做好了，这里只需要查找对应的点即可 */
        /* 找出所有不能被prove的点 */
        for (Set<Compound> fact_set: globalFunctor2FactSetMap.values()) {
            for (Compound fact : fact_set) {
                GraphNode4Compound fact_node = new GraphNode4Compound(fact);
                if (!graph.containsKey(fact_node)) {
                    startSet.add(fact);
                }
            }
        }

        /* 找出所有SCC中的覆盖点 */
        final int start_set_size_without_scc = startSet.size();
        int scc_total_vertices = 0;
        int fvs_total_vertices = 0;
        Tarjan<GraphNode4Compound> tarjan = new Tarjan<>(graph);
        List<Set<GraphNode4Compound>> sccs = tarjan.run();
        for (Set<GraphNode4Compound> scc: sccs) {
            /* 找出FVS的一个解，并把之放入start_set */
            FeedbackVertexSetSolver<GraphNode4Compound> fvs_solver = new FeedbackVertexSetSolver<>(graph, scc);
            Set<GraphNode4Compound> fvs = fvs_solver.run();
            for (GraphNode4Compound node: fvs) {
                startSet.add(node.compound);
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
    protected List<Rule> dumpHypothesis() {
        return hypothesis;
    }

    @Override
    protected Set<Compound> dumpStartSet() {
        return startSet;
    }

    @Override
    protected Set<Compound> dumpCounterExampleSet() {
        return counterExamples;
    }

    @Override
    public boolean validate() {
        /* Todo: Implement Here */
        return false;
    }

    public static void main(String[] args) {
        SincFullyOptimized compressor = new SincFullyOptimized(
                EvalMetric.CompressionCapacity,
                "FamilyRelationMedium(0.00)(10x).tsv",
                null,
                null,
                null,
                false
        );
        compressor.run();
    }
}
