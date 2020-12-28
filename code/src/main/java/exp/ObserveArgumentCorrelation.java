package exp;

import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ObserveArgumentCorrelation {

    private final String bkPath;
    private Map<String, MultiSet<String>[]> predicate2ArgumentSetMap = new HashMap<>();

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     */
    private void loadBk() {
        try {
            System.out.println("\n>>> Loading BK...");
            BufferedReader reader = new BufferedReader(new FileReader(bkPath));
            String line;
            int line_cnt = 0;
            while (null != (line = reader.readLine())) {
                String[] components = line.split("\t");
                MultiSet<String>[] arg_set_list =  predicate2ArgumentSetMap.computeIfAbsent(components[0], k -> {
                    MultiSet<String>[] _arg_set_list = new MultiSet[components.length - 1];
                    for (int i = 0; i < _arg_set_list.length; i++) {
                        _arg_set_list[i] = new MultiSet<>();
                    }
                    return _arg_set_list;
                });
                for (int i = 1; i < components.length; i++) {
                    arg_set_list[i-1].add(components[i]);
                }
                line_cnt++;
            }
            System.out.printf("BK loaded: %d predicates; %d facts\n", predicate2ArgumentSetMap.size(), line_cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void computeJaccardMatrix() {

    }
}
