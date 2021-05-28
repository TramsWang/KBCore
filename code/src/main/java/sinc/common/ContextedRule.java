package sinc.common;

import sinc.util.DisjointSet;
import sinc.util.MemKB;

import java.util.*;

public class ContextedRule {
    public static final int HEAD_PRED_IDX = 0;
    public static final int FIRST_BODY_PRED_IDX = HEAD_PRED_IDX + 1;
    public static final int CONSTANT_ARG_ID = -1;

    private final List<Predicate> structure;
    private final List<Variable> boundedVars;  // Bounded vars use non-negative ids(list index)
    private final List<Integer> boundedVarCnts;
    private RuleFingerPrint fingerPrint;
    private int equivConds;
    private Eval eval;

    /* 记录符合条件的grounding的中间结果 */
    private static class PredicateCache {
        public final Predicate predicate;
        public Set<Predicate> inclusion;

        public PredicateCache(Predicate predicate) {
            this.predicate = predicate;
            this.inclusion = new HashSet<>();
        }

        public PredicateCache(PredicateCache another) {
            this.predicate = new Predicate(another.predicate);
            this.inclusion = another.inclusion;
        }
    }
    private final MemKB kb;
    private final Set<ContextedRule> cache;
    private final List<List<PredicateCache>> groundings = new LinkedList<>();
    private final List<Set<Predicate>> exclusion = new ArrayList<>();
    private final List<List<PredicateCache>> groundingsBody = new LinkedList<>();
    private final List<Set<Predicate>> exclusionBody = new ArrayList<>();

    /* 查询辅助信息类 */
//    private static class PredArgPos {
//        final int predIdx;
//        final int argIdx;
//
//        public PredArgPos(int predIdx, int argIdx) {
//            this.predIdx = predIdx;
//            this.argIdx = argIdx;
//        }
//    }

    public ContextedRule(MemKB kb, String headFunctor, Set<ContextedRule> cache) {
        structure = new ArrayList<>();
        boundedVars = new ArrayList<>();
        boundedVarCnts = new ArrayList<>();
        fingerPrint = new RuleFingerPrint(structure);
        equivConds = 0;
        eval = null;
        this.kb = kb;
        this.cache = cache;

        /* 把无BV的head加入 */
        final Predicate head = new Predicate(headFunctor, kb.getArity(headFunctor));
        structure.add(head);
        final PredicateCache head_cache = new PredicateCache(new Predicate(headFunctor, head.arity()));
        head_cache.inclusion.addAll(kb.getAllFacts(headFunctor));
        final List<PredicateCache> grounding = new ArrayList<>();
        grounding.add(head_cache);
        groundings.add(grounding);
        exclusion.add(new HashSet<>());

//        final PredicateCache head_cache_body = new PredicateCache(new Predicate(headFunctor, head.arity()));
        final List<PredicateCache> grounding_body = new ArrayList<>();
        grounding_body.add(null);
        groundingsBody.add(grounding_body);
        exclusionBody.add(null);
    }

    public ContextedRule(ContextedRule another) {
        /* Todo: Implement Here */
        this.structure = new ArrayList<>(another.structure.size());
        for (Predicate predicate: another.structure) {
            this.structure.add(new Predicate(predicate));
        }
        this.boundedVars = new ArrayList<>(another.boundedVars);
        this.boundedVarCnts = new ArrayList<>(another.boundedVarCnts);
        this.fingerPrint = another.fingerPrint;
        this.equivConds = another.equivConds;
        this.eval = another.eval;
        this.kb = another.kb;
        this.cache = another.cache;
    }

    public Predicate getPredicate(int idx) {
        return structure.get(idx);
    }

    public Predicate getHead() {
        return structure.get(HEAD_PRED_IDX);
    }

    public int length() {
        return structure.size();
    }

    public int usedBoundedVars() {
        return boundedVars.size();
    }

    public int size() {
        return equivConds;
    }

    public Eval getEval() {
        return eval;
    }

    /**
     * 以下几种情况为Invalid：
     *   1. Cache中已经存在
     *   2. Trivial
     *   3. Independent Fragment
     *
     * @return
     */
    private boolean isInvalid() {
        /* 在Cache中已经有了，就不用再计算了 */
        if (!cache.add(this)) {
            return true;
        }

        /* Independent Fragment(可能在找origin的时候出现) */
        /* 用并查集检查 */
        /* Assumption: 没有全部是Free Var或Const的Pred(除了head)，因此把所有Bounded Var根据在一个Pred里出现进行合并即可 */
        DisjointSet disjoint_set = new DisjointSet(boundedVars.size());

        /* Trivial(用Set检查) */
        /* 1. 用Set检查 */
        /* 2. 为了防止进入和Head重复的情况，检查和Head存在相同位置相同参数的情况 */
        Predicate head_pred = structure.get(0);
        {
            /* 先把Head中的变量进行统计加入disjoint set */
            List<Integer> bounded_var_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                Argument argument = head_pred.args[arg_idx];
                if (null != argument && argument.isVar) {
                    bounded_var_ids.add(argument.id);
                }
            }
            if (bounded_var_ids.isEmpty()) {
                if (structure.size() >= 2) {
                    /* Head中没有bounded var但是body不为空，此时head是一个independent fragment */
                    return true;
                }
            } else {
                /* 这里必须判断，因为Head中可能不存在Bounded Var */
                int first_id = bounded_var_ids.get(0);
                for (int i = 1; i < bounded_var_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, bounded_var_ids.get(i));
                }
            }
        }

        Set<Predicate> predicate_set = new HashSet<>();
        for (int pred_idx = 1; pred_idx < structure.size(); pred_idx++) {
            Predicate body_pred = structure.get(pred_idx);
            if (head_pred.functor.equals(body_pred.functor)) {
                for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                    Argument head_arg = head_pred.args[arg_idx];
                    Argument body_arg = body_pred.args[arg_idx];
                    if (null != head_arg && head_arg.equals(body_arg)) {
                        return true;
                    }
                }
            }

            boolean args_complete = true;
            List<Integer> bounded_var_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                Argument argument = body_pred.args[arg_idx];
                if (null == argument) {
                    args_complete = false;
                } else if (argument.isVar) {
                    bounded_var_ids.add(argument.id);
                }
            }

            if (args_complete) {
                if (!predicate_set.add(body_pred)) {
                    return true;
                }
            }

            /* 在同一个Predicate中出现的Bounded Var合并到一个集合中 */
            if (bounded_var_ids.isEmpty()) {
                /* 如果body的pred中没有bounded var那一定是independent fragment */
                return true;
            } else {
                int first_id = bounded_var_ids.get(0);
                for (int i = 1; i < bounded_var_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, bounded_var_ids.get(i));
                }
            }
        }

        /* 判断是否存在Independent Fragment */
        return 2 <= disjoint_set.totalSets();
    }

    /**
     * 将当前已有的一个FV绑定成一个已有的BV
     *
     * @param predIdx
     * @param argIdx
     * @param varId
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public boolean boundFreeVar2ExistingVar(final int predIdx, final int argIdx, final int varId) {
        /* 改变Rule结构 */
        final Predicate target_predicate = structure.get(predIdx);
        target_predicate.args[argIdx] = boundedVars.get(varId);
        boundedVarCnts.set(varId, boundedVarCnts.get(varId)+1);
        equivConds++;
        fingerPrint = new RuleFingerPrint(structure);

        /* 检查合法性 */
        if (isInvalid()) {
            return false;
        }

        /* 更新整体Rule的Cache */
        boundFreeVar2ExistingVarUpdateCache(predIdx, argIdx, varId, false);

        /* 更新只含Body的Cache */
        boundFreeVar2ExistingVarUpdateCache(predIdx, argIdx, varId, true);

        /* 更新Eval */
        updateEval();
        return true;
    }

    /**
     * 在对应的情况下更新Cache
     * @param predIdx
     * @param argIdx
     * @param varId
     * @param bodyOnly
     */
    private void boundFreeVar2ExistingVarUpdateCache(
            final int predIdx, final int argIdx, final int varId, boolean bodyOnly
    ) {
        final int pred_idx_start;
        final List<List<PredicateCache>> grounding_list;
        final List<Set<Predicate>> exclusion_list;
        if (bodyOnly) {
            if (HEAD_PRED_IDX == predIdx) {
                /* 修改不涉及body的时候，body的cache不需要更新 */
                return;
            }
            pred_idx_start = FIRST_BODY_PRED_IDX;
            grounding_list = groundingsBody;
            exclusion_list = exclusionBody;
        } else {
            pred_idx_start = HEAD_PRED_IDX;
            grounding_list = groundings;
            exclusion_list = exclusion;
        }

        final Predicate target_predicate = structure.get(predIdx);
        final Set<String> possible_substitutions = kb.getValueSet(target_predicate.functor, argIdx);
        boolean found = false;
        for (int pred_idx = pred_idx_start; pred_idx < structure.size() && !found; pred_idx++) {
            final Predicate predicate = structure.get(pred_idx);

            /* 找到一个predicate对应当前BV的一个arg的位置 */
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                final Argument argument = predicate.args[arg_idx];
                if (null != argument && argument.isVar && varId == argument.id &&
                        (pred_idx != predIdx || arg_idx != argIdx)) {  // 不要和刚设置的变量比较
                    found = true;

                    /* 更新变量可用值 */
                    final Set<String> original_substitutions = new HashSet<>();
                    for (List<PredicateCache> grounding : grounding_list) {
                        original_substitutions.add(grounding.get(pred_idx).predicate.args[arg_idx].name);
                    }
                    final Set<String> filtered_substitutions = setIntersection(
                            possible_substitutions, original_substitutions
                    );

                    /* 根据当前pred过滤grounding */
                    final Iterator<List<PredicateCache>> grounding_itr = grounding_list.iterator();
                    while (grounding_itr.hasNext()) {
                        final List<PredicateCache> grounding = grounding_itr.next();
                        final PredicateCache pred_cache = grounding.get(pred_idx);
                        final Argument grounding_argument = pred_cache.predicate.args[arg_idx];
                        boolean grounding_invalid = false;

                        /* 如果变量值不在可选范围内，当前grounding不可取，删除 */
                        if (!filtered_substitutions.contains(grounding_argument.name)) {
                            grounding_invalid = true;
                        } else {
                            /* 根据新绑定的参数列过滤FV的集合 */
                            final PredicateCache target_pred_cache = grounding.get(predIdx);
                            final Iterator<Predicate> fv_itr = target_pred_cache.inclusion.iterator();
                            while (fv_itr.hasNext()) {
                                final Predicate fv_pred = fv_itr.next();
                                final Argument fv_arg = fv_pred.args[argIdx];
                                if (!grounding_argument.name.equals(fv_arg.name)) {
                                    fv_itr.remove();
                                }
                            }

                            /* 如果过滤之后FV集合为空，那么说明当前的grounding也不能用 */
                            if (target_pred_cache.inclusion.isEmpty()) {
                                grounding_invalid = true;
                            } else {
                                /* 如果当前grounding仍然满足要求，则更新对应参数 */
                                target_pred_cache.predicate.args[argIdx] = grounding_argument;
                            }
                        }

                        if (grounding_invalid) {
                            grounding_itr.remove();
                            for (int pred_idx2 = pred_idx_start; pred_idx2 < structure.size(); pred_idx2++) {
                                exclusion_list.get(pred_idx2).addAll(grounding.get(pred_idx2).inclusion);
                            }
                        }
                    }
                    break;
                }
            }
        }

        if (bodyOnly && !found) {
            /* 如果body中没有找到其他相同的BV，那么目标参数对应的列应该按照所有值展开inclusion并设置对应的值 */
            final ListIterator<List<PredicateCache>> grounding_itr = grounding_list.listIterator();
            while (grounding_itr.hasNext()) {
                final List<PredicateCache> grounding = grounding_itr.next();
                final PredicateCache target_pred_cache = grounding.get(predIdx);

                /* 按目标列的值划分inclusion */
                final Map<String, Set<Predicate>> inclusion_map = new HashMap<>();
                for (Predicate predicate: target_pred_cache.inclusion) {
                    inclusion_map.compute(predicate.args[argIdx].name, (constant, set) -> {
                        if (null == set) {
                            set = new HashSet<>();
                        }
                        set.add(predicate);
                        return set;
                    });
                }

                /* 展开grounding */
                if (1 == inclusion_map.size()) {
                    /* 目标参数处只有一个值，直接修改grounding中对应参数即可 */
                    target_pred_cache.predicate.args[argIdx] = new Constant(
                            CONSTANT_ARG_ID, inclusion_map.keySet().iterator().next()
                    );
                } else {
                    /* 用多个grounding替代原有grounding */
                    grounding_itr.remove();
                    for (Map.Entry<String, Set<Predicate>> entry: inclusion_map.entrySet()) {
                        final List<PredicateCache> new_grounding = dupGrounding(grounding, true);
                        final PredicateCache new_target_pred_cace = new_grounding.get(predIdx);
                        new_target_pred_cace.predicate.args[argIdx] = new Constant(CONSTANT_ARG_ID, entry.getKey());
                        new_target_pred_cace.inclusion = entry.getValue();
                        grounding_itr.add(new_grounding);
                    }
                }
            }
        }
    }

    /**
     * 新添加一个Predicate，然后将其中的一个FV绑定成一个已有的BV
     *
     * @param functor
     * @param argIdx
     * @param varId
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public boolean boundFreeVar2ExistingVar(String functor, int argIdx, int varId) {
        /* 改变Rule结构 */

        /* 检查合法性 */

        /* 更新Cache */

        /* 更新Eval */

        /* Todo: Implement Here */
        return false;
    }

    /**
     * 将两个已有的FV绑定成同一个新的BV
     *
     * @param predIdx1
     * @param argIdx1
     * @param predIdx2
     * @param argIdx2
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public boolean boundFreeVars2NewVar(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        /* 改变Rule结构 */

        /* 检查合法性 */

        /* 更新Cache */

        /* 更新Eval */

        /* Todo: Implement Here */
        return false;
    }

    /**
     * 添加一个新的Predicate，然后将其中的一个FV以及一个已有的FV绑定成同一个新的BV
     *
     * @param functor
     * @param argIdx1
     * @param predIdx2
     * @param argIdx2
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public boolean boundFreeVars2NewVar(String functor, int argIdx1, int predIdx2, int argIdx2) {
        /* 改变Rule结构 */

        /* 检查合法性 */

        /* 更新Cache */

        /* 更新Eval */

        /* Todo: Implement Here */
        return false;
    }

    /**
     * 将一个已有的FV绑定成常量
     *
     * @param predIdx
     * @param argIdx
     * @param constId
     * @param constant
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public boolean boundFreeVar2Constant(int predIdx, int argIdx, int constId, String constant) {
        /* 改变Rule结构 */

        /* 检查合法性 */

        /* 更新Cache */

        /* 更新Eval */

        /* Todo: Implement Here */
        return false;
    }

    /**
     * 将对应位置的BV或Const条件删除，
     * @param predIdx
     * @param argIdx
     * @return
     */
    public boolean removeBoundedArg(int predIdx, int argIdx) {
        /* 改变Rule结构 */

        /* 检查合法性 */

        /* 更新Cache */

        /* 更新Eval */

        /* Todo: Implement Here */
        return false;
    }

    private void updateEval() {
        /* Todo: Implement Here */
    }

    private <T> HashSet<T> setIntersection(Set<T> s1, Set<T> s2) {
        HashSet<T> result = new HashSet<>();
        if (s1.size() <= s2.size()) {
            for (T t: s1) {
                if (s2.contains(t)) {
                    result.add(t);
                }
            }
        } else {
            for (T t: s2) {
                if (s1.contains(t)) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    private List<PredicateCache> dupGrounding(List<PredicateCache> grounding, boolean bodyOnly) {
        List<PredicateCache> new_grounding = new ArrayList<>(grounding.size());
        if (bodyOnly) {
            new_grounding.add(null);
            for (int i = 1; i < grounding.size(); i++) {
                new_grounding.add(new PredicateCache(grounding.get(i)));
            }
        } else {
            for (PredicateCache pred_cache: grounding) {
                new_grounding.add(new PredicateCache(pred_cache));
            }
        }
        return new_grounding;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("(");
        builder.append(eval).append(')');
        builder.append(structure.get(0).toString()).append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString());
            }
        }
        return builder.toString();
    }

    public String toCompleteRuleString() {
        /* 先把Free vars都加上 */
        List<Predicate> copy = new ArrayList<>(this.structure);
        int free_vars = boundedVars.size();
        for (Predicate predicate: copy) {
            for (int i = 0; i < predicate.arity(); i++) {
                if (null == predicate.args[i]) {
                    predicate.args[i] = new Variable(free_vars);
                    free_vars++;
                }
            }
        }

        /* to string without eval */
        StringBuilder builder = new StringBuilder(copy.get(0).toString());
        builder.append(":-");
        if (1 < copy.size()) {
            builder.append(structure.get(1).toString());
            for (int i = 2; i < copy.size(); i++) {
                builder.append(',').append(copy.get(i).toString());
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextedRule rule1 = (ContextedRule) o;
        return this.fingerPrint.equals(rule1.fingerPrint);
    }

    @Override
    public int hashCode() {
        return fingerPrint.hashCode();
    }
}
