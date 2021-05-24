package exp.statistic;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import sinc.common.*;
import sinc.util.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class RuleStatistic {
    public static List<Predicate> parseRule(String str) {
        /* 形如：head(...):-body1(...),...,bodyn(...) */
        String[] two_parts = str.split(":-");
        String head = two_parts[0];
        String[] bodies = (two_parts.length == 1) ? new String[0] :
                two_parts[1].split("[)],");
        List<Predicate> rule = new ArrayList<>();
        rule.add(parsePredicate(head));
        for (String body_str: bodies) {
            rule.add(parsePredicate(body_str));
        }
        return rule;
    }

    public static Predicate parsePredicate(String str) {
        /* 形如：functor(arg1,...,argn)*/
        String[] components = str.split("[(,)]");
        Predicate predicate = new Predicate(components[0], components.length - 1);
        for (int i = 1; i < components.length; i++) {
            String arg = components[i];
            char first_char = arg.charAt(0);
            switch (first_char) {
                case 'X':
                    /* Bounded Var */
                    int id = Integer.parseInt(arg.substring(1));
                    predicate.args[i-1] = new Variable(id);
                    break;
                case '?':
                    /* Free Var */
                    predicate.args[i-1] = null;
                    break;
                default:
                    /* Constant */
                    predicate.args[i-1] = new Constant(-1, arg);
            }
        }
        return predicate;
    }

    public static void run(String bkFilePath) throws IOException {
        Map<String, MultiSet<String>[]> functor2ArgSetsMap = new HashMap<>();
//        List<List<Predicate>> rules = new ArrayList<>();

        /* Load Facts */
        BufferedReader reader = new BufferedReader(new FileReader(bkFilePath));
        String line;
        while (null != (line = reader.readLine())) {
            String[] components = line.split("\t");
            String functor = components[0];
            MultiSet<String>[] arg_set_list =  functor2ArgSetsMap.computeIfAbsent(functor, k -> {
                MultiSet<String>[] _arg_set_list = new MultiSet[components.length - 1];
                for (int i = 0; i < _arg_set_list.length; i++) {
                    _arg_set_list[i] = new MultiSet<>();
                }
                return _arg_set_list;
            });
            for (int i = 1; i < components.length && i <= arg_set_list.length; i++) {
                arg_set_list[i-1].add(components[i]);
            }
        }
        reader.close();

//        /* Load Rules */
//        reader = new BufferedReader(new FileReader(rulePath));
//        while (null != (line = reader.readLine())) {
//            rules.add(parseRule(line));
//        }

        /* 统计 */
        int conditions = 0;
        int compares = 0;
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        List<Double> jaccards = new ArrayList<>();
        String[] functors = functor2ArgSetsMap.keySet().toArray(new String[0]);
        for (int fi = 0; fi < functors.length; fi++) {
            String functor_i = functors[fi];
            MultiSet<String>[] arg_sets_i = functor2ArgSetsMap.get(functor_i);
            for (int fj = fi; fj < functors.length; fj++) {
                String functor_j = functors[fj];
                MultiSet<String>[] arg_sets_j = functor2ArgSetsMap.get(functor_j);
                for (int arg_idx_i = 0; arg_idx_i < arg_sets_i.length; arg_idx_i++) {
                    for (int arg_idx_j = 0; arg_idx_j < arg_sets_j.length; arg_idx_j++) {
                        double jaccard = arg_sets_i[arg_idx_i].jaccardSimilarity(
                                arg_sets_j[arg_idx_j]
                        );
                        statistics.addValue(jaccard);
                        jaccards.add(jaccard);
                    }
                }
            }
        }


//        for (List<Predicate> rule: rules) {
//            /* Find Equiv-Classes */
//            Map<Integer, Set<VarIndicator>> equiv_classes = new HashMap<>();
//            for (Predicate predicate: rule) {
//                for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
//                    Argument argument = predicate.args[arg_idx];
//                    int col = arg_idx;
//                    if (null != argument && argument.isVar) {
//                        equiv_classes.compute(argument.id, (k, v) -> {
//                            if (null == v) {
//                                v = new HashSet<>();
//                            }
//                            v.add(new VarIndicator(predicate.functor, col));
//                            return v;
//                        });
//                    }
//                }
//            }
//
//            /* Calculate Jaccard */
//            for (Set<VarIndicator> indicator_set: equiv_classes.values()) {
//                conditions += indicator_set.size() - 1;
//                compares += indicator_set.size() * (indicator_set.size() - 1) / 2;
//                VarIndicator[] indicators = indicator_set.toArray(new VarIndicator[0]);
//                for (int i = 0; i < indicators.length; i++) {
//                    VarIndicator vi = indicators[i];
//                    MultiSet<String> consts_i = functor2ArgSetsMap.get(vi.functor)[vi.idx];
//                    for (int j = i + 1; j < indicators.length; j++) {
//                        VarIndicator vj = indicators[j];
//                        MultiSet<String> consts_j = functor2ArgSetsMap.get(vj.functor)[vj.idx];
//                        double jaccard = consts_i.jaccardSimilarity(consts_j);
//                        statistics.addValue(jaccard);
//                        jaccards.add(jaccard);
//                    }
//                }
//            }
//        }

        /* Output */
        System.out.printf("KB: %s\n", bkFilePath);
        System.out.println(Arrays.toString(jaccards.toArray(new Double[0])));
//        System.out.println("---");
//        System.out.printf("%10s %10s %10s\n", "Rules", "Conds", "Compares");
//        System.out.printf("%10d %10d %10d\n", rules.size(), conditions, compares);
        System.out.println("---");
        System.out.printf("%10s %10s %10s %10s %10s %10s\n", "#Jaccard", "25%", "50%", "75%", "avg", "max");
        System.out.printf("%10s %10.4f %10.4f %10.4f %10.4f %10.4f\n", "",
                statistics.getPercentile(25),
                statistics.getPercentile(50),
                statistics.getPercentile(75),
                statistics.getMean(),
                statistics.getMax()
        );
        System.out.println("---");
        System.out.println();
    }

    private static void testParser() throws Exception {
        String rule_str = "sister(X0,e):-mother(X0,?),sister(X1,X0),male(X1)";
        List<Predicate> rule = parseRule(rule_str);
        StringBuilder builder = new StringBuilder(rule.get(0).toString()).append(":-");
        if (2 <= rule.size()) {
            builder.append(rule.get(1).toString());
            for (int i = 2; i < rule.size(); i++) {
                builder.append(',').append(rule.get(i).toString());
            }
        }
        String actual = builder.toString();
        if (!rule_str.equals(actual)) {
            System.out.println(rule_str);
            System.out.println(actual);
            throw new Exception("Parse Failed");
        }
    }

    public static void main(String[] args) throws Exception {
        testParser();
        String[] bk_paths = new String[]{
                "testData/familyRelation/FamilyRelationSimple(0.00)(10x).tsv",
                "testData/familyRelation/FamilyRelationMedium(0.00)(10x).tsv",
                "testData/RKB/Elti.tsv",
                "testData/RKB/Dunur.tsv",
                "testData/RKB/StudentLoan.tsv",
                "testData/RKB/dbpedia_factbook.tsv",
                "testData/RKB/dbpedia_lobidorg.tsv",
                "testData/RKB/webkb.cornell.tsv",
                "testData/RKB/webkb.texas.tsv",
                "testData/RKB/webkb.washington.tsv",
                "testData/RKB/webkb.wisconsin.tsv",
        };

        for (String bk_path: bk_paths) {
            run(bk_path);
        }
    }
}
