package exp.statistic;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import sinc.common.VarIndicator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConstantStatistic {
    static void run(String bkFilePath) throws IOException  {
        Map<String, Integer> constant_occurrences = new HashMap<>();
        Map<String, Set<VarIndicator>> constant_locations = new HashMap<>();
        Set<String> functors = new HashSet<>();
        int facts = 0;

        /* Load KB */
        BufferedReader reader = new BufferedReader(new FileReader(bkFilePath));
        String line;
        while (null != (line = reader.readLine())) {
            String[] components = line.split("\t");
            String functor = components[0];
            functors.add(functor);
            facts++;

            for (int i = 1; i < components.length; i++) {
                String constant = components[i];
                int idx = i - 1;
                constant_occurrences.compute(constant, (k, v) -> (null == v)? 1: v + 1);
                constant_locations.compute(constant, (k, v) -> {
                    if (null == v) {
                        v = new HashSet<>();
                    }
                    v.add(new VarIndicator(functor, idx));
                    return v;
                });
            }
        }

        /* Calculate Statistics */
        System.out.printf("%s Loaded\n", bkFilePath);
        System.out.println("---");
        System.out.printf("%10s %10s %10s\n", "Functors", "Constants", "Facts");
        System.out.printf("%10d %10d %10d\n", functors.size(), constant_locations.keySet().size(), facts);
        System.out.println("---");

        DescriptiveStatistics stat_occurrences = new DescriptiveStatistics();
        List<Integer> occurrences = new ArrayList<>();
        DescriptiveStatistics stat_locations = new DescriptiveStatistics();
        List<Integer> locations = new ArrayList<>();
        for (int cnt: constant_occurrences.values()) {
            stat_occurrences.addValue(cnt);
            occurrences.add(cnt);
        }
        for (Set<VarIndicator> locs: constant_locations.values()) {
            stat_locations.addValue(locs.size());
            locations.add(locs.size());
        }
        System.out.println("---");
        System.out.println("Occurrences:");
        System.out.println(Arrays.toString(occurrences.toArray(new Integer[0])));
        System.out.println("Locations:");
        System.out.println(Arrays.toString(locations.toArray(new Integer[0])));
        System.out.println("---");
        System.out.printf("%10s %10s %10s %10s %10s %10s\n", "#Occur", "25%", "50%", "75%", "avg", "max");
        System.out.printf("%10s %10.4f %10.4f %10.4f %10.4f %10.4f\n", "",
                stat_occurrences.getPercentile(25),
                stat_occurrences.getPercentile(50),
                stat_occurrences.getPercentile(75),
                stat_occurrences.getMean(),
                stat_occurrences.getMax()
        );
        System.out.println("---");
        System.out.printf("%10s %10s %10s %10s %10s %10s\n", "#Loc", "25%", "50%", "75%", "avg", "max");
        System.out.printf("%10s %10.4f %10.4f %10.4f %10.4f %10.4f\n", "",
                stat_locations.getPercentile(25),
                stat_locations.getPercentile(50),
                stat_locations.getPercentile(75),
                stat_locations.getMean(),
                stat_locations.getMax()
        );
        System.out.println("---");
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
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
