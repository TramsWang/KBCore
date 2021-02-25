package sinc.impl;

import org.jpl7.*;
import org.jpl7.Variable;
import sinc.SInC;
import sinc.common.*;
import sinc.util.MultiSet;
import sinc.util.PrologModule;
import sinc.util.SwiplUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.*;

public class SincFullyOptimized extends SInC {

    protected static final double MIN_HEAD_COVERAGE = 0.05;
    protected static final double MIN_CONSTANT_PROPORTION = 0.25;
    protected static final int DEFAULT_CONST_ID = -1;

    protected final Set<Compound> globalFacts = new HashSet<>();
    protected final Map<String, Integer> functor2ArityMap = new HashMap<>();

    protected final Map<String, Set<Compound>> curFunctor2FactSetMap = new HashMap<>();
    protected final Map<String, MultiSet<String>[]> curFunctor2ArgSetsMap = new HashMap<>();
    protected final Set<String> constants = new HashSet<>();

    protected final List<Rule> hypothesis = new ArrayList<>();

    protected boolean shouldContinue = true;

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
    protected void loadBk() {
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
                }
                Compound compound = new Compound(functor, args);
                SwiplUtil.appendKnowledge(PrologModule.GLOBAL, compound);
                globalFacts.add(compound);
//                curFacts.add(compound);
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

            System.out.printf(
                    "BK loaded: %d predicates; %d constants, %d facts\n",
                    curFunctor2FactSetMap.size(), constants.size(), globalFacts.size()
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
    protected Rule findRule() {
        Set<String> ignore_set = new HashSet<>();
        if (debug) {
            ignore_set.add("gender");
        }

        /* 初始化Evaluation Cache */
        Map<Rule, Eval> eval_cache = new HashMap<>();

        /* 找到仅有head的rule中得分最高的作为起始rule */
        List<Rule> starting_rules = new ArrayList<>();
        for (Map.Entry<String, Integer> entry: functor2ArityMap.entrySet()) {
            String predicate = entry.getKey();
            Integer arity = entry.getValue();
            if (ignore_set.contains(predicate)) {
                continue;
            }
            Rule rule = new Rule(predicate, arity);
            checkThenAddRule(starting_rules, rule, eval_cache);
        }
        if (starting_rules.isEmpty()) {
            /* 没有适合条件的规则了 */
            return null;
        }
        Rule r_max = starting_rules.get(0);
        for (Rule r: starting_rules) {
            if (r.getEval().value(evalType) > r_max.getEval().value(evalType)) {
                r_max = r;
            }
        }

        /* 计算所有符合阈值的constant */
        Map<String, List<String>[]> functor_2_promising_const_map = new HashMap<>();
        for (Map.Entry<String, MultiSet<String>[]> entry: curFunctor2ArgSetsMap.entrySet()) {
            MultiSet<String>[] arg_sets = entry.getValue();
            List<String>[] arg_const_lists = new List[arg_sets.length];
            for (int i = 0; i < arg_sets.length; i++) {
                arg_const_lists[i] = arg_sets[i].elementsAboveProportion(MIN_CONSTANT_PROPORTION);
            }
            functor_2_promising_const_map.put(entry.getKey(), arg_const_lists);
        }

        /* 寻找局部最优（只要进入这个循环，一定有局部最优） */
        while (true) {
            System.out.printf("Extend: %s\n", r_max);
            Rule r_e_max = r_max;

            /* 遍历r_max的扩展邻居 */
            List<Rule> extensions = findExtension(r_max, functor_2_promising_const_map, eval_cache);
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
        Set<Compound> facts = curFunctor2FactSetMap.get(head_pred.functor);

        /* 如果head上的所有变量都是自由变量则直接计算 */
        if (head_pred.arity() == free_var_cnt_in_head) {
            rule.setEval(
                    new Eval(
                            facts.size(), Math.pow(constants.size(), head_pred.arity()), rule.size()
                    )
            );
            return;
        }

        /* 计算entailments */
        Compound head_compound = new Compound(head_pred.functor, head_args);
        StringBuilder query_builder = new StringBuilder(head_compound.toString());
        for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
            Predicate body_pred = rule.getPredicate(pred_idx);
            Term[] args = new Term[body_pred.arity()];
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                Argument argument = body_pred.args[arg_idx];
                args[arg_idx] = (null == argument) ? new Variable("_") :
                        argument.isVar ? new Variable(argument.name) : new Atom(argument.name);
            }
            Compound body_compound = new Compound(body_pred.functor, args);
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
            rule.setEval(Eval.MIN);
            return;
        }

        rule.setEval(
                new Eval(
                        pos_cnt,
                        head_templates.size() * Math.pow(constants.size(), free_var_cnt_in_head),
                        rule.size()
                )
        );
    }

    protected List<Rule> findExtension(
            Rule rule, Map<String, List<String>[]> functor2promisingConstMap, Map<Rule, Eval> evalCache
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
            List<String> const_list = functor2promisingConstMap.get(predicate.functor)[first_vacant[1]];
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
               搜索空间中的，但是实际eval的时候会将这种规则当做空的规则，因此不会成为r_max，也就不会影响搜索结果 */
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
        if (null == rule || !rule.getEval().useful(evalType)) {
            shouldContinue = false;
            return;
        }

        hypothesis.add(rule);
        dumpHypothesis();

        /* 删除已经被证明的 */
        Predicate head_predicate = rule.getHead();
        Term[] head_args = new Term[head_predicate.args.length];
        int free_var_cnt_in_head = 0;
        for (int arg_idx = 0; arg_idx < head_args.length; arg_idx++) {
            Argument argument = head_predicate.args[arg_idx];
            if (null == argument) {
                head_args[arg_idx] = new Variable(String.format("Y%d", free_var_cnt_in_head));
                free_var_cnt_in_head++;
            } else {
                head_args[arg_idx] = argument.isVar ? new Variable(argument.name) : new Atom(argument.name);
            }
        }
        Compound head_compound = new Compound(head_predicate.functor, head_args);
        StringBuilder query_builder = new StringBuilder(head_compound.toString());
        for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
            Predicate body_predicate = rule.getPredicate(pred_idx);
            Term[] args = new Term[body_predicate.arity()];
            for (int arg_idx = 0; arg_idx < body_predicate.arity(); arg_idx++) {
                Argument argument = body_predicate.args[arg_idx];
                args[arg_idx] = (null == argument) ? new Variable("_") :
                        argument.isVar ? new Variable(argument.name) : new Atom(argument.name);
            }
            Compound body_compound = new Compound(body_predicate.functor, args);
            query_builder.append(',').append(body_compound.toString());
        }
        String query_str = query_builder.toString();
        Query q = new Query(":", new Term[]{
                new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(query_str)
        });

        Set<Compound> fact_set = curFunctor2FactSetMap.get(head_predicate.functor);
        int removed_cnt = 0;
        for (Map<String, Term> binding: q) {
            Compound head_instance = SwiplUtil.substitute(head_compound, binding);
            if (fact_set.remove(head_instance)) {
                removed_cnt++;
            }
        }
        System.out.printf("Update: %d removed\n", removed_cnt);
        q.close();
    }

    @Override
    protected void dumpHypothesis() {
        System.out.println("\nHypothesis Found:");
        for (Rule rule: hypothesis) {
            System.out.println(rule);
        }
    }

    @Override
    protected void dumpStartSet() {
        /* Todo: Implement Here */
    }

    @Override
    protected void dumpCounterExampleSet() {
        /* Todo: Implement Here */
    }

    public static void main(String[] args) {
        SincFullyOptimized compressor = new SincFullyOptimized(
                EvalMetric.CompressRatio,
                "FamilyRelationMedium(0.00)(10x).tsv",
                null,
                null,
                null,
                true
        );
        compressor.run();
    }
}
