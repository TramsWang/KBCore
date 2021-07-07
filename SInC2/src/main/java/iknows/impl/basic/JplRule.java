package iknows.impl.basic;

import org.jpl7.*;
import org.jpl7.Variable;
import iknows.common.*;

import java.lang.Integer;
import java.util.*;

public class JplRule extends Rule {

    private final PrologKb kb;

    /* 性能检测数据 */
    static protected final JplQueryMonitor jplQueryMonitor = new JplQueryMonitor();

    public JplRule(String headFunctor, Set<RuleFingerPrint> cache, PrologKb kb) {
        super(headFunctor, kb.getArity(headFunctor), cache);
        this.kb = kb;
        this.eval = calculateEval();
    }

    public JplRule(JplRule another) {
        super(another);
        this.kb = another.kb;
    }

    @Override
    public Rule clone() {
        return new JplRule(this);
    }

    @Override
    protected double factCoverage() {
        /* Todo: Implement Here */
        throw new Error("Not Implemented");
    }

    @Override
    protected Eval calculateEval() {
        /* 统计Head的参数情况，并将其转成带Free Var的Jpl Arg Array */
        final long pre_computing_start = System.nanoTime();
        final Predicate head_pred = getHead();
        final List<String> bounded_vars_in_head = new ArrayList<>();
        final Term[] head_args = new Term[head_pred.args.length];
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
        final Set<Predicate> global_facts = kb.getGlobalFactsByFunctor(head_pred.functor);
        final Set<Predicate> cur_facts = kb.getCurrentFactsByFunctor(head_pred.functor);

        /* 如果head上的所有变量都是自由变量则直接计算 */
        if (head_pred.arity() == free_var_cnt_in_head) {
            return new Eval(
                    eval,
                    cur_facts.size(),
                    Math.pow(kb.totalConstants(), head_pred.arity()) -
                            global_facts.size() + cur_facts.size(),
                    this.size()
            );
        }

        /* 计算all entailments */
        final long all_entailments_query_begin = System.nanoTime();
        final Set<String> bounded_vars_in_body = new HashSet<>();
        final StringBuilder query_builder = new StringBuilder();
        if (2 <= this.length()) {
            for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < this.length(); pred_idx++) {
                final Predicate body_pred = structure.get(pred_idx);
                final Term[] args = new Term[body_pred.arity()];
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
                final Compound body_compound = new Compound(body_pred.functor, args);
                query_builder.append(body_compound).append(',');
            }
            query_builder.deleteCharAt(query_builder.length() - 1);
        }
        String query_str = query_builder.toString();
        final Set<Compound> head_templates = new HashSet<>();
        final boolean body_is_not_empty = !"".equals(query_str);
        if (body_is_not_empty) {
            final long query_begin = System.nanoTime();
            final Query q = new Query(Term.textToTerm(query_str));
            for (Map<String, Term> binding : q) {
                long build_head_template_begin = System.nanoTime();
                Term[] template_args = new Term[bounded_vars_in_head.size()];
                for (int arg_idx = 0; arg_idx < template_args.length; arg_idx++) {
                    template_args[arg_idx] = binding.get(bounded_vars_in_head.get(arg_idx));
                }
                head_templates.add(new Compound("h", template_args));
                long build_head_template_done = System.nanoTime();
                final long period = build_head_template_done - build_head_template_begin;
                jplQueryMonitor.headTemplateBuildCostInNano += period;
                jplQueryMonitor.jplQueryCostInNano -= period;
            }
            q.close();
            long query_done = System.nanoTime();
            jplQueryMonitor.jplQueryCostInNano += query_done - query_begin;
        }
        final Set<String> bounded_vars_in_head_only = new HashSet<>();
        for (String bv_head : bounded_vars_in_head) {
            if (!bounded_vars_in_body.contains(bv_head)) {
                /* 找出所有仅在Head中出现的bounded var */
                bounded_vars_in_head_only.add(bv_head);
            }
        }
        final double all_entailments = (body_is_not_empty ? head_templates.size() : 1) * Math.pow(
                kb.totalConstants(), free_var_cnt_in_head + bounded_vars_in_head_only.size()
        );

        /* 计算positive entailments */
        final Compound head_compound = new Compound(head_pred.functor, head_args);
        query_str = body_is_not_empty ? head_compound.toString() + ',' + query_str :
                head_compound.toString();

        final long positive_entailments_query_begin = System.nanoTime();
        final Query q = new Query(Term.textToTerm(query_str));
        final Set<Predicate> head_instances = new HashSet<>();
        for (Map<String, Term> binding: q) {
            long substitute_begin = System.nanoTime();
            head_instances.add(PrologKb.compound2Fact(PrologKb.substitute(head_compound, binding)));
            long substitute_done = System.nanoTime();
            final long period = substitute_done - substitute_begin;
            jplQueryMonitor.substituteCostInNano += period;
            jplQueryMonitor.jplQueryCostInNano -= period;
        }
        q.close();
        final long positive_entailments_query_done = System.nanoTime();

        int positive_entailments = 0;
        int already_entailed = 0;
        for (Predicate head_instance: head_instances) {
            if (cur_facts.contains(head_instance)) {
                positive_entailments++;
            } else if (global_facts.contains(head_instance)) {
                already_entailed++;
            }
        }
        final long check_done = System.nanoTime();
        jplQueryMonitor.jplQueryCostInNano += positive_entailments_query_done - positive_entailments_query_begin;
        jplQueryMonitor.preComputingCostInNano += all_entailments_query_begin - pre_computing_start;
        jplQueryMonitor.allEntailQueryCostInNano += positive_entailments_query_begin - all_entailments_query_begin;
        jplQueryMonitor.posEntailQueryCostInNano += check_done - positive_entailments_query_begin;

//        /* 用HC剪枝 */
//        double head_coverage = ((double) positive_entailments) / global_facts.size();
//        if (Rule.MIN_FACT_COVERAGE >= head_coverage) {
//            return Eval.MIN;
//        }

        return new Eval(
                eval, positive_entailments, all_entailments - already_entailed, this.size()
        );
    }

    public UpdateResult updateInKb() {
        return new UpdateResult(findGroundings(), findCounterExmaples());
    }

    public List<Predicate[]> findGroundings() {
        final List<Predicate[]> groundings = new ArrayList<>();

        /* 统计Head的参数情况，并将其转成带Free Var的Jpl Compound */
        final Predicate head_pred = this.getHead();
        final List<String> bounded_vars_in_head = new ArrayList<>();
        final Term[] head_args = new Term[head_pred.args.length];
        int free_var_cnt_in_head = 0;
        for (int arg_idx = 0; arg_idx < head_args.length; arg_idx++) {
            final Argument argument = head_pred.args[arg_idx];
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
        final Compound head_compound = new Compound(head_pred.functor, head_args);
        final Set<Predicate> global_facts = kb.getGlobalFactsByFunctor(head_pred.functor);
        final Set<Predicate> cur_fact_set = kb.getCurrentFactsByFunctor(head_pred.functor);

        /* 构造所有的dependency */
        boolean body_is_not_empty = (2 <= this.length());
        if (body_is_not_empty) {
            /* 用body构造查询 */
            final Set<String> bounded_vars_in_body = new HashSet<>();
            final StringBuilder query_builder = new StringBuilder();
            final List<Compound> body_compounds = new ArrayList<>(this.length() - 1);
            for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < this.length(); pred_idx++) {
                final Predicate body_pred = this.getPredicate(pred_idx);
                final Term[] args = new Term[body_pred.arity()];
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
                final Compound body_compound = new Compound(body_pred.functor, args);
                body_compounds.add(body_compound);
                query_builder.append(body_compound).append(',');
            }
            query_builder.deleteCharAt(query_builder.length() - 1);
            final String query_str = query_builder.toString();

            /* 找出所有仅在Head中出现的bounded var */
            final Set<String> bounded_vars_in_head_only = new HashSet<>();
            for (String bv_head : bounded_vars_in_head) {
                if (!bounded_vars_in_body.contains(bv_head)) {
                    bounded_vars_in_head_only.add(bv_head);
                }
            }

            final Query q_4_body_grounding = new Query(Term.textToTerm(query_str));
            for (Map<String, Term> binding : q_4_body_grounding) {
                /* 构造body grounding */
                final List<Compound> body_groundings = new ArrayList<>(body_compounds.size());
                for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < this.length(); pred_idx++) {
                    final Predicate body_pred = this.getPredicate(pred_idx);
                    final Compound body_compound = body_compounds.get(pred_idx - 1);
                    int free_vars_in_body = 0;
                    final Term[] body_args = new Term[body_pred.arity()];
                    for (int arg_idx = 0; arg_idx < body_args.length; arg_idx++) {
                        final Argument argument = body_pred.args[arg_idx];
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
                        final Query q_4_single_body_grounding = new Query(body_grounding);
                        final Map<String, Term> single_solution = q_4_single_body_grounding.nextSolution();
                        q_4_single_body_grounding.close();
                        body_grounding = PrologKb.substitute(body_grounding, single_solution);
                    }
                    body_groundings.add(body_grounding);
                }

                /* 构造head grounding*/
                final Compound head_grounding = PrologKb.substitute(head_compound, binding);
                if (0 >= bounded_vars_in_head_only.size() + free_var_cnt_in_head) {
                    buildGrounding(
                            cur_fact_set, global_facts, groundings, head_grounding, body_groundings
                    );
                } else {
                    /* 如果head中带有free var需要遍历所有可能值 */
                    final Query q_4_head_grounding = new Query(head_grounding);
                    for (Map<String, Term> head_binding: q_4_head_grounding) {
                        buildGrounding(
                                cur_fact_set, global_facts, groundings, PrologKb.substitute(
                                        head_grounding, head_binding
                                ), body_groundings
                        );
                    }
                    q_4_head_grounding.close();
                }
            }
            q_4_body_grounding.close();
        } else {
            /* Body为True(i.e. AXIOM) */
            if (0 >= bounded_vars_in_head.size() + free_var_cnt_in_head) {
                buildGrounding(
                        cur_fact_set, global_facts, groundings, head_compound, new ArrayList<>()
                );
            } else {
                /* 如果head中带有free var需要遍历所有可能值 */
                Query q_4_head_grounding = new Query(head_compound);
                for (Map<String, Term> head_binding: q_4_head_grounding) {
                    buildGrounding(
                            cur_fact_set, global_facts, groundings, PrologKb.substitute(
                                    head_compound, head_binding
                            ), new ArrayList<>()
                    );
                }
                q_4_head_grounding.close();
            }
        }

        return groundings;
    }

    private Set<Predicate> findCounterExmaples() {
        final Set<Predicate> counter_examples = new HashSet<>();

        /* 统计head中的变量信息 */
        final Map<Integer, List<Integer>> head_var_2_loc_map = new HashMap<>();
        int fv_id = boundedVars.size();
        final Predicate head_pred = new Predicate(getHead());
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            final Argument argument = head_pred.args[arg_idx];
            if (null == argument) {
                head_var_2_loc_map.put(fv_id, new ArrayList<>(Collections.singleton(arg_idx)));
                fv_id++;
            } else {
                if (argument.isVar) {
                    final int idx = arg_idx;
                    head_var_2_loc_map.compute(argument.id, (id, locs) -> {
                        if (null == locs) {
                            locs = new ArrayList<>();
                        }
                        locs.add(idx);
                        return locs;
                    });
                }
            }
        }

        /* 用body构造查询 */
        final StringBuilder query_builder = new StringBuilder();
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            final Predicate body_pred = structure.get(pred_idx);
            final Term[] args = new Term[body_pred.arity()];
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                final Argument argument = body_pred.args[arg_idx];
                if (null == argument) {
                    args[arg_idx] = new Variable("_");
                } else if (argument.isVar) {
                    args[arg_idx] = new Variable(argument.name);
                    head_var_2_loc_map.remove(argument.id);
                } else {
                    args[arg_idx] = new Atom(argument.name);
                }
            }
            query_builder.append(new Compound(body_pred.functor, args)).append(',');
        }

        /* 找出所有仅在Head中出现的bounded var */
        final Integer[][] head_only_var_locs = new Integer[head_var_2_loc_map.size()][];
        {
            int i = 0;
            for (List<Integer> loc_list : head_var_2_loc_map.values()) {
                head_only_var_locs[i] = loc_list.toArray(new Integer[0]);
                i++;
            }
        }

        /* 根据Rule结构找Counter Example */
        boolean body_is_not_empty = (2 <= this.length());
        if (body_is_not_empty) {
            /* 找到所有head template */
            query_builder.deleteCharAt(query_builder.length() - 1);
            final String query_str = query_builder.toString();
            final Query q_4_body_grounding = new Query(Term.textToTerm(query_str));
            final Set<Predicate> head_templates = new HashSet<>();
            for (Map<String, Term> binding : q_4_body_grounding) {
                head_templates.add(PrologKb.substitute(head_pred, binding));
            }

            if (0 == head_only_var_locs.length) {
                /* 不需要替换变量 */
                for (Predicate head_template : head_templates) {
                    if (!kb.containsFact(head_template)) {
                        counter_examples.add(head_template);
                    }
                }
            } else {
                /* 需要替换head中的变量 */
                for (Predicate head_template: head_templates) {
                    iterate4CounterExamples(counter_examples, head_template, 0, head_only_var_locs);
                }
            }
        } else {
            /* 没有Body */
            if (0 == head_only_var_locs.length) {
                /* head中全是常量 */
                if (!kb.containsFact(head_pred)) {
                    counter_examples.add(head_pred);
                }
            } else {
                /* head中有变量，而且全部当做自由变量处理 */
                iterate4CounterExamples(counter_examples, head_pred, 0, head_only_var_locs);
            }
        }
        return counter_examples;
    }

    private void iterate4CounterExamples(
            final Set<Predicate> counterExamples, final Predicate template, final int idx,
            final Integer[][] varLocs
    ) {
        final Integer[] locations = varLocs[idx];
        if (idx < varLocs.length - 1) {
            /* 递归 */
            for (String constant_symbol: kb.allConstants()) {
                final Constant constant = new Constant(CONSTANT_ARG_ID, constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = constant;
                }
                iterate4CounterExamples(
                        counterExamples, template, idx + 1, varLocs
                );
            }
        } else {
            /* 已经到了最后的位置，不递归，完成后检查是否是Counter Example */
            for (String constant_symbol: kb.allConstants()) {
                final Constant constant = new Constant(CONSTANT_ARG_ID, constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = constant;
                }
                if (!kb.containsFact(template)) {
                    counterExamples.add(new Predicate(template));
                }
            }
        }
    }

    private void buildGrounding(
            Set<Predicate> curFactSet, Set<Predicate> globalFactSet, List<Predicate[]> groundings,
            Compound head, List<Compound> bodies
    ) {
        final Predicate head_pred = PrologKb.compound2Fact(head);
        if (curFactSet.remove(head_pred)) {
            /* 删除并在graph中加入dependency */
            Predicate[] grounding = new Predicate[bodies.size() + 1];
            grounding[HEAD_PRED_IDX] = head_pred;
            for (int i = FIRST_BODY_PRED_IDX; i < grounding.length; i++) {
                grounding[i] = PrologKb.compound2Fact(bodies.get(i-1));
            }
            groundings.add(grounding);
        }
        /* 否则就是返利或已经被prove过的，忽略即可 */
    }
}
