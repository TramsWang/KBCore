package sinc.impl.basic;

import org.jpl7.*;
import org.jpl7.Variable;
import sinc.common.*;

import java.util.*;

public class JplRule extends Rule {

    private final PrologKb kb;

    /* 性能检测数据 */
    long preComputingCostInNano = 0;
    long allEntailQueryCostInNano = 0;
    long posEntailQueryCostInNano = 0;
    long headTemplateBuildCostInNano = 0;
    long allEntailJplQueryCostInNano = 0;
    long substituteCostInNano = 0;

    public JplRule(String headFunctor, Set<RuleFingerPrint> cache, PrologKb kb) {
        super(headFunctor, kb.getArity(headFunctor), cache);
        this.kb = kb;
        this.eval = calculateEval();
    }

    public JplRule(JplRule another) {
        super(another);
        this.kb = another.kb;
        this.preComputingCostInNano = another.preComputingCostInNano;
        this.allEntailQueryCostInNano = another.allEntailQueryCostInNano;
        this.posEntailQueryCostInNano = another.posEntailQueryCostInNano;
        this.headTemplateBuildCostInNano = another.headTemplateBuildCostInNano;
        this.allEntailJplQueryCostInNano = another.allEntailJplQueryCostInNano;
        this.substituteCostInNano = another.substituteCostInNano;
    }

    @Override
    public Rule clone() {
        return new JplRule(this);
    }

    @Override
    public void boundFreeVar2ExistingVarHandler(int predIdx, int argIdx, int varId) {}

    @Override
    public void boundFreeVar2ExistingVarHandler(Predicate newPredicate, int argIdx, int varId) {}

    @Override
    public void boundFreeVars2NewVarHandler(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {}

    @Override
    public void boundFreeVars2NewVarHandler(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {}

    @Override
    public void boundFreeVar2ConstantHandler(int predIdx, int argIdx, String constantSymbol) {}

    @Override
    public void removeBoundedArgHandler(int predIdx, int argIdx) {}

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
                query_builder.append(body_compound.toString()).append(',');
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
                headTemplateBuildCostInNano += build_head_template_done - build_head_template_begin;
            }
            q.close();
            long query_done = System.nanoTime();
            allEntailJplQueryCostInNano += query_done - query_begin;
        }
        final Set<String> bounded_vars_in_head_only = new HashSet<>();
        for (String bv_head : bounded_vars_in_head) {
            if (!bounded_vars_in_body.contains(bv_head)) {
                /* 找出所有仅在Head中出现的bounded var */
                bounded_vars_in_head_only.add(bv_head);
            }
        }
        // TODO: 这里对bounded_vars_in_head_only相关的数量估计是错误的，它不一定能取所有的const
        final double all_entailments = (body_is_not_empty ? head_templates.size() : 1) * Math.pow(
                kb.totalConstants(), free_var_cnt_in_head + bounded_vars_in_head_only.size()
        );

        /* 计算positive entailments */
        final long positive_entailments_query_begin = System.nanoTime();
        final Compound head_compound = new Compound(head_pred.functor, head_args);
        query_str = body_is_not_empty ? head_compound.toString() + ',' + query_str :
                head_compound.toString();

        final Query q = new Query(Term.textToTerm(query_str));
        final Set<Compound> head_instances = new HashSet<>();
        for (Map<String, Term> binding: q) {
            long substitute_begin = System.nanoTime();
            head_instances.add(PrologKb.substitute(head_compound, binding));
            long substitute_done = System.nanoTime();
            substituteCostInNano += substitute_done - substitute_begin;
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
        final long positive_entailments_query_done = System.nanoTime();
        preComputingCostInNano += all_entailments_query_begin - pre_computing_start;
        allEntailQueryCostInNano += positive_entailments_query_begin - all_entailments_query_begin;
        posEntailQueryCostInNano += positive_entailments_query_done - positive_entailments_query_begin;

        /* 用HC剪枝 */
        double head_coverage = ((double) positive_entailments) / global_facts.size();
        if (Rule.MIN_HEAD_COVERAGE >= head_coverage) {
            return null;
        }
        return new Eval(
                eval, positive_entailments, all_entailments - already_entailed, this.size()
        );
    }

    public UpdateResult updateInKb() {
        final List<Predicate[]> groundings = new ArrayList<>();
        final Set<Predicate> counter_examples = new HashSet<>();

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
                query_builder.append(body_compound.toString()).append(',');
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
                if (bounded_vars_in_head_only.isEmpty()) {
                    buildGrounding(
                            cur_fact_set, global_facts, groundings, counter_examples, head_grounding, body_groundings
                    );
                } else {
                    /* 如果head中带有free var需要遍历所有可能值 */
                    final Query q_4_head_grounding = new Query(head_grounding);
                    for (Map<String, Term> head_binding: q_4_head_grounding) {
                        buildGrounding(
                                cur_fact_set, global_facts, groundings, counter_examples, PrologKb.substitute(
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
            if (0 >= free_var_cnt_in_head) {
                buildGrounding(
                        cur_fact_set, global_facts, groundings, counter_examples, head_compound, new ArrayList<>()
                );
            } else {
                // TODO: 这里有问题，如果规则是： head(X,X):- 则不能找到任何positive example
                /* 如果head中带有free var需要遍历所有可能值 */
                Query q_4_head_grounding = new Query(head_compound);
                for (Map<String, Term> head_binding: q_4_head_grounding) {
                    buildGrounding(
                            cur_fact_set, global_facts, groundings, counter_examples, PrologKb.substitute(
                                    head_compound, head_binding
                            ), new ArrayList<>()
                    );
                }
                q_4_head_grounding.close();
            }
        }

        return new UpdateResult(groundings, counter_examples);
    }

    private void buildGrounding(
            Set<Predicate> curFactSet, Set<Predicate> globalFactSet, List<Predicate[]> groundings,
            Set<Predicate> counterExampleSet, Compound head, List<Compound> bodies
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
        } else if (!globalFactSet.contains(head_pred)) {
            /* 加入反例集合 */
            counterExampleSet.add(head_pred);
        }
        /* 否则就是已经被prove过的，忽略即可 */
    }
}
