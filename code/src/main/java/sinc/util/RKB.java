package sinc.util;

import sinc.common.*;
import sinc.impl.SincBasicWithJPL;

import java.sql.*;
import java.util.*;

public class RKB {
    public static final String PROVED_TABLE_NAME_SUFFIX = "_proved";
    public static final String NULL_VALUE = "NULL";
    public static final int CONSTANT_ID = -1;

    public static final class OriginalFactIterator implements Iterator<Predicate> {
        Iterator<Set<Predicate>> functorItr;
        Iterator<Predicate> factSetItr;

        private OriginalFactIterator(Map<String, Set<Predicate>> functor2FactSetMap) {
            functorItr = functor2FactSetMap.values().iterator();
            factSetItr = functorItr.hasNext() ? functorItr.next().iterator() : null;
        }

        @Override
        public boolean hasNext() {
            return functorItr.hasNext() || (null != factSetItr && factSetItr.hasNext());
        }

        @Override
        public Predicate next() {
            if (!factSetItr.hasNext()) {
                factSetItr = functorItr.next().iterator();
            }
            return factSetItr.next();
        }
    }

    public final String dbName;
    public final double headCoverageThreshold;
    private final Connection connection;
    private final Map<String, Set<Predicate>> functor2FactSetMap = new HashMap<>();
    private final Map<String, Integer> functor2ArityMap = new HashMap<>();
    private final Set<String> constants = new HashSet<>();

    public RKB(String dbName, double headCoverageThreshold) {
        this.dbName = dbName;
        this.headCoverageThreshold = headCoverageThreshold;
        try {
            Class.forName("org.sqlite.JDBC");
            if (null == dbName) {
                connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbName + ".db");
            }
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /* Define two tables:
     *     1. 所有原始的predicate的表
     *     2. 所有已经证明的predicate的表
     */
    public void defineFunctor(String functor, int arity) throws SQLException {
        Statement statement = connection.createStatement();
        StringBuilder sql_builder = new StringBuilder("CREATE TABLE ");
        StringBuilder sql_builder2 = new StringBuilder("CREATE TABLE ");
        sql_builder.append(functor).append(" (");
        sql_builder2.append(functor).append(PROVED_TABLE_NAME_SUFFIX).append(" (");
        if (1 <= arity) {
            sql_builder.append("C0 CHAR(32) NOT NULL");
            sql_builder2.append("C0 CHAR(32) NOT NULL");
            for (int i = 1; i < arity; i++) {
                sql_builder.append(", C").append(i).append(" CHAR(32) NOT NULL");
                sql_builder2.append(", C").append(i).append(" CHAR(32) NOT NULL");
            }
        }
        sql_builder.append(")");
        sql_builder2.append(")");
        String sql = sql_builder.toString();
        String sql2 = sql_builder2.toString();
        statement.executeUpdate(sql);
        statement.executeUpdate(sql2);
        statement.close();
        functor2ArityMap.put(functor, arity);
    }

    public void addPredicate(Predicate predicate) throws SQLException {
        Statement statement = connection.createStatement();
        StringBuilder cmd_builder = new StringBuilder("INSERT INTO ")
                .append(predicate.functor).append(" (");
        StringBuilder value_builder = new StringBuilder("VALUES (");
        if (1 <= predicate.arity()) {
            cmd_builder.append("C0");
            String arg = (null == predicate.args[0]) ? NULL_VALUE : predicate.args[0].name;
            value_builder.append('\'').append(arg).append('\'');
            constants.add(arg);
            for (int i = 1; i < predicate.arity(); i++) {
                cmd_builder.append(",C").append(i);
                arg = (null == predicate.args[i]) ? NULL_VALUE : predicate.args[i].name;
                value_builder.append(',').append('\'').append(arg).append('\'');
                constants.add(arg);
            }
        }
        cmd_builder.append(") ");
        value_builder.append(");");
        String sql = cmd_builder.toString() + value_builder.toString();
        statement.executeUpdate(sql);
        statement.close();

        /* Add to local set */
        functor2FactSetMap.compute(predicate.functor, (k, v) -> {
            if (null == v) {
                v = new HashSet<>();
            }
            v.add(predicate);
            return v;
        });
    }

    public int totalConstants() {
        return constants.size();
    }

    public int totalFacts() {
        int cnt = 0;
        for (Set<Predicate> predicate_set: functor2FactSetMap.values()) {
            cnt += predicate_set.size();
        }
        return cnt;
    }

    public Set<Predicate> listPredicate(String functor, int arity) throws SQLException{
        Statement statement = connection.createStatement();
        ResultSet result_set = statement.executeQuery("SELECT * FROM " + functor + ";");
        Set<Predicate> predicates = new HashSet<>();
        while (result_set.next()) {
            Predicate predicate = new Predicate(functor, arity);
            for (int i = 0; i < arity; i++) {
                predicate.args[i] = new Constant(CONSTANT_ID, result_set.getString("C" + i));
            }
            predicates.add(predicate);
        }
        return predicates;
    }

    public Eval evalRule(Rule rule) throws SQLException {
        /* 统计Head的参数情况，并将其转成带Free Var的Rule */
        Rule rule_with_free_vars = new Rule(rule);
        Predicate head_pred = rule_with_free_vars.getHead();
        Set<Integer> bounded_vars_in_head = new HashSet<>();
        int free_var_id = rule_with_free_vars.usedBoundedVars();
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            Argument argument = head_pred.args[arg_idx];
            if (null == argument) {
                head_pred.args[arg_idx] = new Variable(free_var_id);
                free_var_id++;
            } else if (argument.isVar) {
                bounded_vars_in_head.add(argument.id);
            }
        }
        int free_var_cnt_in_head = free_var_id - rule_with_free_vars.usedBoundedVars();

        /* 将Body中的null转成free var，统计仅在head中的bounded vars */
        Set<Integer> bounded_vars_in_head_only = new HashSet<>(bounded_vars_in_head);
        for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
            Predicate body_pred = rule_with_free_vars.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                Argument argument = body_pred.args[arg_idx];
                if (null == argument) {
                    body_pred.args[arg_idx] = new Variable(free_var_id);
                    free_var_id++;
                } else if (argument.isVar) {
                    bounded_vars_in_head_only.remove(argument.id);
                }
            }
        }

        /* Parse complete rule to get all (+)entails */
        // TODO: 这两个查询可以写成一个吗？提高性能
        String sql4all_pos_tmp = parseSql4AllPosEntailments(rule_with_free_vars);
        String sql4all_pos = String.format(
                "SELECT COUNT(*) FROM (%s)", sql4all_pos_tmp
        );
        String sql4new_pos = String.format(
                "SELECT COUNT(*) FROM (%s EXCEPT SELECT * FROM %s%s)",
                sql4all_pos_tmp, head_pred.functor, PROVED_TABLE_NAME_SUFFIX
        );
        Statement statement = connection.createStatement();
        ResultSet result_set = statement.executeQuery(sql4new_pos);
        final long new_proofs = result_set.getLong(1);

        /* 用HC剪枝 */
        Set<Predicate> global_facts = functor2FactSetMap.get(head_pred.functor);
        double head_coverage = ((double) new_proofs) / global_facts.size();
        if (headCoverageThreshold >= head_coverage) {
            rule.setEval(Eval.MIN);
            return Eval.MIN;
        }

        result_set = statement.executeQuery(sql4all_pos);
        final long all_proofs = result_set.getLong(1);

        /* Parse Body Only to get all entails */
        final double all_entailment_cnt;
        String sql4all_entailments = parseSql4AllEntailments(rule_with_free_vars);
        if (null != sql4all_entailments) {
            /* 有Body，可以查询 */
            String body_sql = String.format(
                    "SELECT COUNT(*) FROM (%s)", sql4all_entailments
            );
            result_set = statement.executeQuery(body_sql);
            long head_patterns = result_set.getLong(1);

            /* 相对于body，head中的自由变量为仅出现在head中的变量 */
            all_entailment_cnt = head_patterns * Math.pow(
                    constants.size(), free_var_cnt_in_head + bounded_vars_in_head_only.size()
            );
        } else {
            /* 没有Body */
            all_entailment_cnt = Math.pow(
                    constants.size(), free_var_cnt_in_head + bounded_vars_in_head.size()
            );
        }

        /* Assign Eval */
        Eval eval = new Eval(
                new_proofs, all_entailment_cnt - (all_proofs - new_proofs), rule.size()
        );
        rule.setEval(eval);
        return eval;
    }

    private String parseSql4AllEntailments(Rule rule) {
        if (1 >= rule.length()) {
            /* 没有body无法查询 */
            return null;
        }

        final StringBuilder select_exp_builder = new StringBuilder("SELECT DISTINCT ");  // length=16
        final int original_select_length = select_exp_builder.length();
        final StringBuilder from_exp_builder = new StringBuilder("FROM ");  // length=5
        final StringBuilder where_exp_builder = new StringBuilder("WHERE ");  // length=6
        final int original_where_length = where_exp_builder.length();
        Map<Integer, ArgIndicator> first_var_info_map = new HashMap<>();

        /* 统计head中的变量 */
        Set<Integer> head_var_ids = new HashSet<>();
        Predicate head_pred = rule.getHead();
        for (Argument arg: head_pred.args) {
            if (null != arg && arg.isVar) {
                head_var_ids.add(arg.id);
            }
        }

        for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
            Predicate body_pred = rule.getPredicate(pred_idx);
            String functor_alias = body_pred.functor + pred_idx;
            from_exp_builder.append(body_pred.functor).append(" AS ").append(functor_alias).append(',');
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                final int col_idx = arg_idx;
                Argument argument = body_pred.args[col_idx];
                if (null != argument) {
                    if (argument.isVar) {
                        first_var_info_map.compute(argument.id, (k, v) -> {
                            if (null == v) {
                                /* 第一次出现变量，放入map即可 */
                                if (head_var_ids.contains(argument.id)) {
                                    select_exp_builder.append(functor_alias).append(".C").append(col_idx)
                                            .append(" AS ").append(argument.name).append(',');
                                }
                                return new VarIndicator(functor_alias, col_idx);
                            } else {
                                /* 再次出现变量，添加等价关系 */
                                where_exp_builder.append(v.functor).append(".C").append(v.idx)
                                        .append('=').append(functor_alias).append(".C").append(col_idx)
                                        .append(" AND ");
                                return v;
                            }
                        });
                    } else {
                        /* 常量则直接添加等价关系 */
                        where_exp_builder.append(functor_alias).append(".C").append(arg_idx)
                                .append("='").append(argument.name).append("' AND ");
                    }
                }
            }
        }

        if (original_select_length >= select_exp_builder.length()) {
            /* body中没有head中出现的变量，即：对head没有约束，无效查询 */
            return null;
        }

        /* 删除多余的标点 */
        select_exp_builder.deleteCharAt(select_exp_builder.length() - 1).append(' ');
        from_exp_builder.deleteCharAt(from_exp_builder.length() - 1).append(' ');
        if (original_where_length >= where_exp_builder.length()) {
            /* WHERE 语句为空 */
            return select_exp_builder.toString() + from_exp_builder.toString();
        } else {
            /* WHERE 语句非空 */
            where_exp_builder.delete(where_exp_builder.length() - 5, where_exp_builder.length());
            return select_exp_builder.toString() + from_exp_builder.toString() + where_exp_builder.toString();
        }
    }

    private String parseSql4AllPosEntailments(Rule rule) {
        final StringBuilder select_exp_builder = new StringBuilder("SELECT DISTINCT ");  // length=16
        final StringBuilder from_exp_builder = new StringBuilder("FROM ");  // length=5
        final StringBuilder where_exp_builder = new StringBuilder("WHERE ");  // length=6
        final int original_where_length = where_exp_builder.length();
        Map<Integer, ArgIndicator> first_var_info_map = new HashMap<>();

        /* 选择head中所有的参数 */
        Predicate head_pred = rule.getHead();
        String head_functor_alias = head_pred.functor + '0';
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            select_exp_builder.append(head_functor_alias).append(".C").append(arg_idx)
                    .append(" AS C").append(arg_idx).append(',');
        }

        for (int pred_idx = 0; pred_idx < rule.length(); pred_idx++) {
            Predicate predicate = rule.getPredicate(pred_idx);
            String functor_alias = predicate.functor + pred_idx;
            from_exp_builder.append(predicate.functor).append(" AS ").append(functor_alias).append(',');
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                final int col_idx = arg_idx;
                Argument argument = predicate.args[col_idx];
                if (null != argument) {
                    if (argument.isVar) {
                        first_var_info_map.compute(argument.id, (k, v) -> {
                            if (null == v) {
                                /* 第一次出现变量，放入map即可 */
                                return new VarIndicator(functor_alias, col_idx);
                            } else {
                                /* 再次出现变量，添加等价关系 */
                                where_exp_builder.append(v.functor).append(".C").append(v.idx)
                                        .append('=').append(functor_alias).append(".C").append(col_idx)
                                        .append(" AND ");
                                return v;
                            }
                        });
                    } else {
                        /* 常量则直接添加等价关系 */
                        where_exp_builder.append(functor_alias).append(".C").append(arg_idx)
                                .append("='").append(argument.name).append("' AND ");
                    }
                }
            }
        }

        /* 删除多余的标点 */
        select_exp_builder.deleteCharAt(select_exp_builder.length() - 1).append(' ');
        from_exp_builder.deleteCharAt(from_exp_builder.length() - 1).append(' ');
        String all_pos_entail_sql;
        if (original_where_length >= where_exp_builder.length()) {
            /* WHERE 语句为空 */
            all_pos_entail_sql = select_exp_builder.toString() + from_exp_builder.toString();
        } else {
            /* WHERE 语句非空 */
            where_exp_builder.delete(where_exp_builder.length() - 5, where_exp_builder.length());
            where_exp_builder.append(' ');
            all_pos_entail_sql = select_exp_builder.toString() + from_exp_builder.toString() + where_exp_builder.toString();
        }

        return all_pos_entail_sql;
    }

    public List<Predicate[]> findGroundings(Rule rule) throws SQLException {
        /* 后续的操作会改动rule中的参数，首先做一个copy */
        rule = new Rule(rule);

        List<Predicate[]> groundings = new ArrayList<>();
        Statement statement = connection.createStatement();
        ResultSet result_set = statement.executeQuery(parseSql4RuleGroundings(rule));
        while (result_set.next()) {
            Predicate[] grounding = new Predicate[rule.length()];
            for (int pred_idx = 0; pred_idx < grounding.length; pred_idx++) {
                grounding[pred_idx] = substitute(rule.getPredicate(pred_idx), result_set);
            }
            groundings.add(grounding);
        }
        return groundings;
    }

    public void addNewProofs(List<Predicate[]> groundings) throws SQLException {
        for (Predicate[] grounding: groundings) {
            Predicate proved_predicate = new Predicate(
                    grounding[0].functor + PROVED_TABLE_NAME_SUFFIX, grounding[0].arity()
            );
            System.arraycopy(grounding[0].args, 0, proved_predicate.args, 0, grounding[0].arity());
            addPredicate(proved_predicate);
        }
    }

    public List<Predicate> findCounterExamples(Rule rule) throws SQLException {
        /* 统计Head的参数情况，并将其转成带Free Var的Rule */
        Rule rule_with_free_vars = new Rule(rule);
        Predicate head_pred = rule_with_free_vars.getHead();
        Set<Integer> vars_in_head_only = new HashSet<>();
        int free_var_id = rule_with_free_vars.usedBoundedVars();
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            Argument argument = head_pred.args[arg_idx];
            if (null == argument) {
                head_pred.args[arg_idx] = new Variable(free_var_id);
                vars_in_head_only.add(free_var_id);
                free_var_id++;
            } else if (argument.isVar) {
                vars_in_head_only.add(argument.id);
            }
        }

        /* 将Body中的null转成free var，统计仅在head中的bounded vars */
        for (int pred_idx = 1; pred_idx < rule.length(); pred_idx++) {
            Predicate body_pred = rule_with_free_vars.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                Argument argument = body_pred.args[arg_idx];
                if (null == argument) {
                    body_pred.args[arg_idx] = new Variable(free_var_id);
                    free_var_id++;
                } else if (argument.isVar) {
                    vars_in_head_only.remove(argument.id);
                }
            }
        }

        /* 统计head中单独出现变量的位置 */
        List<Integer>[] head_only_var_locations;
        if (vars_in_head_only.isEmpty()) {
            head_only_var_locations = null;
        } else {
            Map<Integer, List<Integer>> var_2_locations = new HashMap<>();
            for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                Argument argument = head_pred.args[arg_idx];
                final int col = arg_idx;
                if (argument.isVar && vars_in_head_only.contains(argument.id)) {
                    var_2_locations.compute(argument.id, (k, v) -> {
                        if (null == v) {
                            v = new ArrayList<>();
                        }
                        v.add(col);
                        return v;
                    });
                }
            }
            head_only_var_locations = var_2_locations.values().toArray(new List[0]);
        }

        /* Parse Body Only to get all entails */
        Set<Predicate> fact_set = functor2FactSetMap.get(head_pred.functor);
        List<Predicate> counter_examples = new ArrayList<>();
        Statement statement = connection.createStatement();
        String sql4all_entailments = parseSql4AllEntailments(rule_with_free_vars);
        if (null != sql4all_entailments) {
            /* 有Body，可以查询 */
            ResultSet result_set = statement.executeQuery(sql4all_entailments);
            if (vars_in_head_only.isEmpty()) {
                /* 不需要替换变量 */
                while (result_set.next()) {
                    Predicate entailment = substitute(head_pred, result_set);
                    if (!fact_set.contains(entailment)) {
                        counter_examples.add(entailment);
                    }
                }
            } else {
                /* 需要替换head中的变量 */
                for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                    /* 先把head中不会出现在SQL结果中的变量抹掉，否则会出错 */
                    Argument argument = head_pred.args[arg_idx];
                    if (argument.isVar && vars_in_head_only.contains(argument.id)) {
                        head_pred.args[arg_idx] = new Constant(CONSTANT_ID, "?");
                    }
                }

                while (result_set.next()) {
                    Predicate head_template = substitute(head_pred, result_set);
                    iterateOverConstants2FindCounterExamples(
                            head_only_var_locations, 0, head_template, fact_set, counter_examples
                    );
                }
            }
        } else {
            /* 没有Body */
            if (vars_in_head_only.isEmpty()) {
                /* 只有head自己 */
                if (!fact_set.contains(head_pred)) {
                    counter_examples.add(new Predicate(head_pred));
                }
            } else {
                iterateOverConstants2FindCounterExamples(
                        head_only_var_locations, 0, head_pred, fact_set, counter_examples
                );
            }
        }

        return counter_examples;
    }

    private void iterateOverConstants2FindCounterExamples(
            List<Integer>[] varLocations, int idx, Predicate template, Set<Predicate> facts,
            List<Predicate> counterExamples
    ) {
        List<Integer> locations = varLocations[idx];
        if (idx < varLocations.length - 1) {
            /* 递归 */
            for (String constant: constants) {
                for (int loc: locations) {
                    template.args[loc] = new Constant(CONSTANT_ID, constant);
                }
                iterateOverConstants2FindCounterExamples(
                        varLocations, idx+1, template, facts, counterExamples
                );
            }
        } else {
            /* 已经到了最后的位置，不递归，完成后检查是否是Counter Example */
            for (String constant: constants) {
                for (int loc: locations) {
                    template.args[loc] = new Constant(CONSTANT_ID, constant);
                }
                if (!facts.contains(template)) {
                    counterExamples.add(new Predicate(template));
                }
            }
        }
    }

    private String parseSql4RuleGroundings(Rule rule) {
        final StringBuilder select_exp_builder = new StringBuilder("SELECT DISTINCT ");  // length=16
        final int original_select_length = select_exp_builder.length();
        final StringBuilder from_exp_builder = new StringBuilder("FROM ");  // length=5
        final StringBuilder where_exp_builder = new StringBuilder("WHERE ");  // length=6
        final int original_where_length = where_exp_builder.length();
        final StringBuilder group_by_exp_builder = new StringBuilder("GROUP BY ");
        final int original_group_by_length = group_by_exp_builder.length();
        Map<Integer, ArgIndicator> first_var_info_map = new HashMap<>();
        int free_var_id = rule.usedBoundedVars();

        for (int pred_idx = 0; pred_idx < rule.length(); pred_idx++) {
            Predicate predicate = rule.getPredicate(pred_idx);
            String functor_alias = predicate.functor + pred_idx;
            from_exp_builder.append(predicate.functor).append(" AS ").append(functor_alias).append(',');
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                final int col_idx = arg_idx;
                Argument argument = predicate.args[col_idx];
                if (null != argument) {
                    if (argument.isVar) {
                        first_var_info_map.compute(argument.id, (k, v) -> {
                            if (null == v) {
                                /* 第一次出现变量，放入map即可 */
                                select_exp_builder.append(functor_alias).append(".C").append(col_idx)
                                        .append(" AS ").append(argument.name).append(',');
                                return new VarIndicator(functor_alias, col_idx);
                            } else {
                                /* 再次出现变量，添加等价关系 */
                                where_exp_builder.append(v.functor).append(".C").append(v.idx)
                                        .append('=').append(functor_alias).append(".C").append(col_idx)
                                        .append(" AND ");
                                return v;
                            }
                        });
                    } else {
                        /* 常量则直接添加等价关系 */
                        where_exp_builder.append(functor_alias).append(".C").append(arg_idx)
                                .append("='").append(argument.name).append("' AND ");
                    }
                } else {
                    /* 自由变量 */
                    Variable free_var = new Variable(free_var_id);
                    predicate.args[arg_idx] = free_var;
                    free_var_id++;
                    select_exp_builder.append(functor_alias).append(".C").append(col_idx)
                            .append(" AS ").append(free_var.name).append(',');
                }
            }
        }

        if (original_select_length >= select_exp_builder.length()) {
            /* rule中没有变量，在SELECT语句中添加一点内容，使得查询能够进行 */
            select_exp_builder.append(rule.getHead().functor).append("0.C0 AS C0,");
        }

        /* 对Head中的变量进行group */
        Predicate head_pred = rule.getHead();
        Set<Integer> head_var_ids = new HashSet<>();
        for (Argument argument: head_pred.args) {
            if (argument.isVar && head_var_ids.add(argument.id)) {
                group_by_exp_builder.append(argument.name).append(',');
            }
        }
        if (original_group_by_length >= group_by_exp_builder.length()) {
            /* head中没有变量，不需要group by语句 */
            group_by_exp_builder.setLength(0);
        } else {
            /* 调整多余标点 */
            group_by_exp_builder.deleteCharAt(group_by_exp_builder.length() - 1);
        }

        /* 删除多余的标点 */
        select_exp_builder.deleteCharAt(select_exp_builder.length() - 1).append(' ');
        from_exp_builder.deleteCharAt(from_exp_builder.length() - 1).append(' ');
        if (original_where_length >= where_exp_builder.length()) {
            /* WHERE 语句为空 */
            return select_exp_builder.toString() + from_exp_builder.toString() +
                    group_by_exp_builder.toString();
        } else {
            /* WHERE 语句非空 */
            where_exp_builder.delete(where_exp_builder.length() - 5, where_exp_builder.length())
                    .append(' ');
            return select_exp_builder.toString() + from_exp_builder.toString() +
                    where_exp_builder.toString() + group_by_exp_builder.toString();
        }
    }

    private Predicate substitute(Predicate predicate, ResultSet varMap) throws SQLException {
        Predicate substituted = new Predicate(predicate.functor, predicate.arity());
        for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
            Argument argument = predicate.args[arg_idx];
            substituted.args[arg_idx] = argument.isVar ? new Constant(
                    CONSTANT_ID, varMap.getString(argument.name)
            ) : argument;
        }
        return substituted;
    }

    public Set<Predicate> findUnprovedPredicates() throws SQLException {
        Set<Predicate> unproved_set = new HashSet<>();
        Statement statement = connection.createStatement();
        for (Map.Entry<String, Integer> entry: functor2ArityMap.entrySet()) {
            String functor = entry.getKey();
            int arity = entry.getValue();
            String sql = String.format(
                    "SELECT * FROM %s EXCEPT SELECT * FROM %s%s", functor, functor, PROVED_TABLE_NAME_SUFFIX
            );
            ResultSet result_set = statement.executeQuery(sql);
            while (result_set.next()) {
                Predicate predicate = new Predicate(functor, arity);
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    predicate.args[arg_idx] = new Constant(CONSTANT_ID, result_set.getString(arg_idx+1));
                }
                unproved_set.add(predicate);
            }
        }
        statement.close();
        return unproved_set;
    }

    public Iterator<Predicate> originalFactIterator() {
        return new OriginalFactIterator(functor2FactSetMap);
    }
}
