package compressor;

import common.GraphNode4Compound;
import common.JplRule;
import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;
import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CompressorWithMultisetDynamicRuleConstruction {
    private enum PrologModule {
        GLOBAL("global"), CURRENT("current"), START_SET("start_set");

        private final String sessionName;

        PrologModule(String sessionName) {
            this.sessionName = sessionName;
        }

        public String getSessionName() {
            return sessionName;
        }
    }

    static class SimilarityInfo {
        double similarity;
        String pred1;
        int pred1ArgIdx;
        String pred2;
        int pred2ArgIdx;

        public SimilarityInfo(double similarity, String pred1, int pred1ArgIdx, String pred2, int pred2ArgIdx) {
            this.similarity = similarity;
            this.pred1 = pred1;
            this.pred1ArgIdx = pred1ArgIdx;
            this.pred2 = pred2;
            this.pred2ArgIdx = pred2ArgIdx;
        }
    }

    enum ArgType {
        CONST, VAR
    }

    class ArgInfo {
        final String name;
        final ArgType type;

        public ArgInfo(String name, ArgType type) {
            this.name = name;
            this.type = type;
        }
    }

    class PredInfo {
        final String predicate;
        final ArgInfo[] args;

        public PredInfo(String predicate, int arity) {
            this.predicate = predicate;
            args = new ArgInfo[arity];
        }
    }

    class RawRule {
        final PredInfo head;
        final List<PredInfo> body;

        public RawRule(PredInfo head) {
            this.head = head;
            body = new ArrayList<>();
        }

        public JplRule toJplRule() {
            /* TODO: Not Implemented */
            return null;
        }
    }

    private final String bkPath;
    private final String hypothesisPath;
    private final boolean debug;

    private final Set<Compound> originalFacts = new HashSet<>();
//    private final Set<Compound> currentFacts = new HashSet<>();
    private final Map<String, Set<Compound>> curFactsByPred = new HashMap<>();
    private final Map<String, MultiSet<String>[]> pred2ArgSetMap = new HashMap<>();
    private final Map<String, Integer> pred2ArityMap = new HashMap<>();

    private final List<JplRule> rules = new ArrayList<>();
    private final Map<GraphNode4Compound, Set<GraphNode4Compound>> graph = new HashMap<>();
    private final Set<Compound> counterExamples = new HashSet<>();
    private final Set<Compound> startSet = new HashSet<>();

    public CompressorWithMultisetDynamicRuleConstruction(String bkPath, String hypothesisPath, boolean debug) {
        this.bkPath = bkPath;
        this.hypothesisPath = hypothesisPath;
        this.debug = debug;
    }

    private void appendKnowledge(PrologModule module, Term knowledge) {
        Query q = new Query(
                new Compound(":", new Term[]{
                        new Atom(module.getSessionName()), new Compound("assertz", new Term[]{knowledge})
                })
        );
        q.hasSolution();
        q.close();
    }

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     *
     * Assume no duplication.
     */
    private void loadBk() {
        try {
            System.out.println("\n>>> Loading BK...");
            BufferedReader reader = new BufferedReader(new FileReader(bkPath));
            String line;
            int line_cnt = 0;
            while (null != (line = reader.readLine())) {
                String[] components = line.split("\t");
                MultiSet<String>[] arg_set_list =  pred2ArgSetMap.computeIfAbsent(components[0], k -> {
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
                String predicate = components[0];
                Compound compound = new Compound(predicate, args);
                appendKnowledge(PrologModule.GLOBAL, compound);
                originalFacts.add(compound);
                appendKnowledge(PrologModule.CURRENT, compound);
                curFactsByPred.computeIfAbsent(predicate, k -> new HashSet<>()).add(compound);
                line_cnt++;
            }
            for (Map.Entry<String, MultiSet<String>[]> entry: pred2ArgSetMap.entrySet()) {
                pred2ArityMap.put(entry.getKey(), entry.getValue().length);
            }
            System.out.printf("BK loaded: %d predicates; %d facts\n", pred2ArgSetMap.size(), line_cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JplRule generateRule() {
        /* 找到当前数量最多的Predicate为Head */
        String max_pred = null;
        int max_cnt = 0;
        for (Map.Entry<String, Set<Compound>> entry: curFactsByPred.entrySet()) {
            if (entry.getValue().size() > max_cnt) {
                max_pred = entry.getKey();
            }
        }

        /* 不断找和当前已经构造的规则中的自由参数最接近的参数进行绑定，找到规则 */
        Map<String, MultiSet<String>[]> filtered_pred_2_arg_set_map = new HashMap<>();
        Map<String, MultiSet<String>[]> other_pred_2_arg_set_map = new HashMap<>(pred2ArgSetMap);
        filtered_pred_2_arg_set_map.put(max_pred, other_pred_2_arg_set_map.remove(max_pred));
        RawRule raw_rule = new RawRule(new PredInfo(max_pred, pred2ArityMap.get(max_pred)));
        for (int i = 0; i < raw_rule.head.args.length; i++) {
            raw_rule.head.args[i] = new ArgInfo(String.format("X%d", i), ArgType.VAR);
        }
        while (true) {
            /* TODO: Implement Here */
        }

        return raw_rule.toJplRule();
    }
}
