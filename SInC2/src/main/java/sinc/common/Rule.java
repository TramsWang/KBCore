package sinc.common;

import sinc.util.DisjointSet;

import java.util.*;

public abstract class Rule {
    public static final int HEAD_PRED_IDX = 0;
    public static final int FIRST_BODY_PRED_IDX = HEAD_PRED_IDX + 1;
    public static final int CONSTANT_ARG_ID = -1;

    public static double MIN_FACT_COVERAGE = 0.0;

    public enum UpdateStatus {
        NORMAL, DUPLICATED, INVALID, INSUFFICIENT_COVERAGE
    }

    protected final List<Predicate> structure;
    protected final List<Variable> boundedVars;  // Bounded vars use non-negative ids(list index)
    protected final List<Integer> boundedVarCnts;
    protected RuleFingerPrint fingerPrint;
    protected int equivConds;
    protected Eval eval;
    protected final Set<RuleFingerPrint> searchedFingerprints;

    public Rule(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints) {
        structure = new ArrayList<>();
        boundedVars = new ArrayList<>();
        boundedVarCnts = new ArrayList<>();

        final Predicate head = new Predicate(headFunctor, arity);
        structure.add(head);

        fingerPrint = new RuleFingerPrint(structure);
        equivConds = 0;
        eval = null;

        this.searchedFingerprints = searchedFingerprints;
        this.searchedFingerprints.add(fingerPrint);
    }

    public Rule(Rule another) {
        this.structure = new ArrayList<>(another.structure.size());
        for (Predicate predicate: another.structure) {
            this.structure.add(new Predicate(predicate));
        }
        this.boundedVars = new ArrayList<>(another.boundedVars);
        this.boundedVarCnts = new ArrayList<>(another.boundedVarCnts);
        this.fingerPrint = another.fingerPrint;
        this.equivConds = another.equivConds;
        this.eval = another.eval;
        this.searchedFingerprints = another.searchedFingerprints;
    }

    public abstract Rule clone();

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
     *   1. Trivial
     *   2. Independent Fragment
     */
    protected boolean isInvalid() {
        /* Independent Fragment(可能在找origin的时候出现) */
        /* 用并查集检查 */
        /* Assumption: 没有全部是Free Var或Const的Pred(除了head)，因此把所有Bounded Var根据在一个Pred里出现进行合并即可 */
        DisjointSet disjoint_set = new DisjointSet(usedBoundedVars());

        /* Trivial(用Set检查) */
        /* 1. 用Set检查 */
        /* 2. 为了防止进入和Head重复的情况，检查和Head存在相同位置相同参数的情况 */
        Predicate head_pred = structure.get(HEAD_PRED_IDX);
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
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
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
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public UpdateStatus boundFreeVar2ExistingVar(
            final int predIdx, final int argIdx, final int varId
    ) {
        /* 改变Rule结构 */
        final Predicate target_predicate = structure.get(predIdx);
        target_predicate.args[argIdx] = boundedVars.get(varId);
        boundedVarCnts.set(varId, boundedVarCnts.get(varId)+1);
        equivConds++;
        fingerPrint = new RuleFingerPrint(structure);

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(predIdx, argIdx, varId);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    protected UpdateStatus boundFreeVar2ExistingVarHandler(
            final int predIdx, final int argIdx, final int varId
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * 新添加一个Predicate，然后将其中的一个FV绑定成一个已有的BV
     *
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public UpdateStatus boundFreeVar2ExistingVar(
            final String functor, final int arity, final int argIdx, final int varId
    ) {
        /* 改变Rule结构 */
        final Predicate target_predicate = new Predicate(functor, arity);
        structure.add(target_predicate);
        target_predicate.args[argIdx] = boundedVars.get(varId);
        boundedVarCnts.set(varId, boundedVarCnts.get(varId)+1);
        equivConds++;
        fingerPrint = new RuleFingerPrint(structure);

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(target_predicate, argIdx, varId);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    protected UpdateStatus boundFreeVar2ExistingVarHandler(
            final Predicate newPredicate, final int argIdx, final int varId
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * 将两个已有的FV绑定成同一个新的BV
     *
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public UpdateStatus boundFreeVars2NewVar(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* 改变Rule结构 */
        final Predicate target_predicate1 = structure.get(predIdx1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final Variable new_var = new Variable(boundedVars.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        boundedVars.add(new_var);
        boundedVarCnts.add(2);
        equivConds++;
        fingerPrint = new RuleFingerPrint(structure);

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(predIdx1, argIdx1, predIdx2, argIdx2);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    protected UpdateStatus boundFreeVars2NewVarHandler(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * 添加一个新的Predicate，然后将其中的一个FV以及一个已有的FV绑定成同一个新的BV
     *
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public UpdateStatus boundFreeVars2NewVar(
            final String functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* 改变Rule结构 */
        final Predicate target_predicate1 = new Predicate(functor, arity);
        structure.add(target_predicate1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final Variable new_var = new Variable(boundedVars.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        boundedVars.add(new_var);
        boundedVarCnts.add(2);
        equivConds++;
        fingerPrint = new RuleFingerPrint(structure);

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(target_predicate1, argIdx1, predIdx2, argIdx2);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    protected UpdateStatus boundFreeVars2NewVarHandler(
            final Predicate newPredicate, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * 将一个已有的FV绑定成常量
     *
     * @return 绑定合理且新规则未曾计算过，返回true；否则返回false
     */
    public UpdateStatus boundFreeVar2Constant(final int predIdx, final int argIdx, final String constantSymbol) {
        /* 改变Rule结构 */
        final Predicate predicate = structure.get(predIdx);
        predicate.args[argIdx] = new Constant(CONSTANT_ARG_ID, constantSymbol);
        equivConds++;
        fingerPrint = new RuleFingerPrint(structure);

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ConstantHandler(predIdx, argIdx, constantSymbol);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    protected UpdateStatus boundFreeVar2ConstantHandler(final int predIdx, final int argIdx, final String constantSymbol) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus removeBoundedArg(final int predIdx, final int argIdx) {
        final Predicate predicate = structure.get(predIdx);
        final Argument argument = predicate.args[argIdx];
        predicate.args[argIdx] = null;

        /* 如果删除的是变量，需要调整相关变量的次数和编号 */
        if (argument.isVar) {
            final Integer var_uses_cnt = boundedVarCnts.get(argument.id);
            if (2 >= var_uses_cnt) {
                /* 用最后一个var填补删除var的空缺 */
                /* 要注意删除的也可能是最后一个var */
                int last_var_idx = boundedVars.size() - 1;
                Variable last_var = boundedVars.remove(last_var_idx);
                boundedVarCnts.set(argument.id, boundedVarCnts.get(last_var_idx));
                boundedVarCnts.remove(last_var_idx);

                /* 删除本次出现以外，还需要再删除作为自由变量的存在 */
                for (Predicate another_predicate : structure) {
                    for (int i = 0; i < another_predicate.arity(); i++) {
                        if (null != another_predicate.args[i]) {
                            if (argument.id == another_predicate.args[i].id) {
                                another_predicate.args[i] = null;
                            }
                        }
                    }
                }

                if (argument != last_var) {
                    for (Predicate another_predicate : structure) {
                        for (int i = 0; i < another_predicate.arity(); i++) {
                            if (null != another_predicate.args[i]) {
                                if (last_var.id == another_predicate.args[i].id) {
                                    another_predicate.args[i] = argument;
                                }
                            }
                        }
                    }
                }
            } else {
                /* 只删除本次出现 */
                boundedVarCnts.set(argument.id, var_uses_cnt - 1);
            }
        }
        equivConds--;

        /* 删除变量可能出现纯自由的predicate，需要一并删除(head保留) */
        Iterator<Predicate> itr = structure.iterator();
        Predicate head_pred = itr.next();
        while (itr.hasNext()) {
            Predicate body_pred = itr.next();
            boolean is_empty_pred = true;
            for (Argument arg_info: body_pred.args) {
                if (null != arg_info) {
                    is_empty_pred = false;
                    break;
                }
            }
            if (is_empty_pred) {
                itr.remove();
            }
        }

        fingerPrint = new RuleFingerPrint(structure);

        /* 检查是否命中Cache */
        if (!searchedFingerprints.add(fingerPrint)) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        /* 执行handler */
        final UpdateStatus status = removeBoundedArgHandler(predIdx, argIdx);
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        return UpdateStatus.NORMAL;
    }

    protected UpdateStatus removeBoundedArgHandler(final int predIdx, final int argIdx) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    protected abstract double factCoverage();

    /**
     * @return 如果不符合Head Coverage，返回Eval.MIN
     */
    protected abstract Eval calculateEval();

    public RuleFingerPrint getFingerPrint() {
        return fingerPrint;
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
        /* Todo: 可以想办法把FV的name做区别化处理，变成Y0, Y1... 与X0, X1... 区别开 */
        List<Predicate> copy = new ArrayList<>(this.structure.size());
        for (Predicate predicate: structure) {
            copy.add(new Predicate(predicate));
        }
        int free_id = usedBoundedVars();
        for (Predicate predicate: copy) {
            for (int i = 0; i < predicate.arity(); i++) {
                if (null == predicate.args[i]) {
                    predicate.args[i] = new Variable(free_id);
                    free_id++;
                }
            }
        }

        /* to string without eval */
        StringBuilder builder = new StringBuilder(copy.get(0).toString());
        builder.append(":-");
        if (1 < copy.size()) {
            builder.append(copy.get(1).toString());
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
        Rule that = (Rule) o;
        return this.fingerPrint.equals(that.fingerPrint);
    }

    @Override
    public int hashCode() {
        return fingerPrint.hashCode();
    }
}
