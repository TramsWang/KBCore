package sinc.util;

import sinc.common.*;

import java.sql.*;
import java.util.*;

public class RKB {
    public static final String PROVED_TABLE_NAME_SUFFIX = "_proved";
    public static final String NULL_VALUE = "NULL";
    public static final int CONSTANT_ID = -1;

    public final String dbName;
    private final Connection connection;
    private final Set<String> constants = new HashSet<>();

    public RKB(String dbName) throws ClassNotFoundException, SQLException {
        this.dbName = dbName;
        Class.forName("org.sqlite.JDBC");
        if (null == dbName) {
            connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        } else {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbName + ".db");
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

        /* Parse Body Only to get all entails */
        Statement statement = connection.createStatement();
        final double all_entailment_cnt;
        String sql4all_entailments = parseSql4AllEntailments(rule_with_free_vars);
        if (null != sql4all_entailments) {
            /* 有Body，可以查询 */
            String body_sql = String.format(
                    "SELECT COUNT(*) FROM (%s)", sql4all_entailments
            );
            ResultSet result_set = statement.executeQuery(body_sql);
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

        /* Parse complete rule to get all (+)entails */
        String complete_sql = String.format(
                "SELECT COUNT(*) FROM (%s)", parseSql4UnprovedPosEntailments(rule_with_free_vars)
        );
        ResultSet result_set = statement.executeQuery(complete_sql);
        final long new_proofs = result_set.getLong(1);

        /* Assign Eval */
        Eval eval = new Eval(new_proofs, all_entailment_cnt, rule.size());
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

    private String parseSql4UnprovedPosEntailments(Rule rule) {
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

        /* 排除已经证明过的 */
        return all_pos_entail_sql + "EXCEPT SELECT * FROM " + head_pred.functor + PROVED_TABLE_NAME_SUFFIX;
    }

    public List<Predicate[]> findGroundingsAndSetAsProved(Rule rule) throws SQLException {
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

    public List<Predicate> findCounterExamples(Rule rule) throws SQLException {
        List<Predicate> counter_examples = new ArrayList<>();
        Predicate head_pred = rule.getHead();
        Statement statement = connection.createStatement();
        ResultSet result_set = statement.executeQuery(parseSql4CounterExamples(rule));
        while (result_set.next()) {
            counter_examples.add(substitute(head_pred, result_set));
        }
        return counter_examples;
    }

    private String parseSql4RuleGroundings(Rule rule) {
        final StringBuilder select_exp_builder = new StringBuilder("SELECT DISTINCT ");  // length=16
        final int original_select_length = select_exp_builder.length();
        final StringBuilder from_exp_builder = new StringBuilder("FROM ");  // length=5
        final StringBuilder where_exp_builder = new StringBuilder("WHERE ");  // length=6
        final int original_where_length = where_exp_builder.length();
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
                    select_exp_builder.append(functor_alias).append(".C").append(col_idx);
                    if (1 <= pred_idx) {
                        /* Body中的自由变量取一个值就可以 */
                        select_exp_builder.append(" AS FIRST_VALUE(").append(free_var.name).append("),");
                    } else {
                        select_exp_builder.append(" AS ").append(free_var.name).append(',');
                    }
                }
            }
        }

        if (original_select_length >= select_exp_builder.length()) {
            /* rule中没有变量，在SELECT语句中添加一点内容，使得查询能够进行 */
            select_exp_builder.append(rule.getHead().functor).append("0.C0 AS C0,");
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
            where_exp_builder.append(' ');
            return select_exp_builder.toString() + from_exp_builder.toString() + where_exp_builder.toString();
        }
    }

    private String parseSql4CounterExamples(Rule rule) {
        final StringBuilder select_exp_builder = new StringBuilder("SELECT DISTINCT ");  // length=16
        final int original_select_length = select_exp_builder.length();
        final StringBuilder from_exp_builder = new StringBuilder("FROM ");  // length=5
        final StringBuilder where_exp_builder = new StringBuilder("WHERE ");  // length=6
        final int original_where_length = where_exp_builder.length();
        Map<Integer, ArgIndicator> first_var_info_map = new HashMap<>();

        /* 选择head中所有的变量 */
        Predicate head_pred = rule.getHead();
        String head_functor_alias = head_pred.functor + '0';
        from_exp_builder.append(head_pred.functor).append(" AS ").append(head_functor_alias).append(',');
        int free_var_id = rule.usedBoundedVars();
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            final int col_idx = arg_idx;
            Argument argument = head_pred.args[col_idx];
            if (null != argument) {
                if (argument.isVar) {
                    first_var_info_map.compute(argument.id, (k, v) -> {
                        if (null == v) {
                            /* 第一次出现变量，放入map即可 */
                            select_exp_builder.append(head_functor_alias).append(".C").append(col_idx)
                                    .append(" AS ").append(argument.name).append(',');
                            return new VarIndicator(head_functor_alias, col_idx);
                        } else {
                            /* 再次出现变量，添加等价关系 */
                            where_exp_builder.append(v.functor).append(".C").append(v.idx)
                                    .append('=').append(head_functor_alias).append(".C").append(col_idx)
                                    .append(" AND ");
                            return v;
                        }
                    });
                } else {
                    /* 常量则直接添加等价关系 */
                    where_exp_builder.append(head_functor_alias).append(".C").append(arg_idx)
                            .append("='").append(argument.name).append("' AND ");
                }
            } else {
                /* 自由变量 */
                Variable free_var = new Variable(free_var_id);
                head_pred.args[arg_idx] = free_var;
                free_var_id++;
                select_exp_builder.append(head_functor_alias).append(".C").append(col_idx)
                        .append(" AS ").append(free_var.name).append(',');
            }
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

        if (original_select_length >= select_exp_builder.length()) {
            /* rule中没有变量，在SELECT语句中添加一点内容，使得查询能够进行 */
            select_exp_builder.append(head_functor_alias).append(".C0 AS C0,");
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

        /* 排除已经证明过的 */
        return all_pos_entail_sql + "EXCEPT SELECT * FROM " + head_pred.functor;
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
}
