package compressor.multiset.dynamic;

import common.GraphNode4Compound;
import common.JplRule;
import compressor.CompressorWithMultisetDynamicRuleConstruction;
import compressor.NaiveCompressor;
import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;
import utils.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Compressor {

    private final String bkPath;
    private final String hypothesisPath;
    private final boolean debug;

    private final Set<Compound> originalFacts = new HashSet<>();
    private final Map<String, Set<Compound>> curFactsByPred = new HashMap<>();
    private final Map<String, MultiSet<String>[]> pred2ArgSetMap = new HashMap<>();
    private final Map<String, Integer> pred2ArityMap = new HashMap<>();

    public Compressor(String bkPath, String hypothesisPath, boolean debug) {
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

    public void observeFirstRoundRules() {
        loadBk();

        for (String pred: pred2ArityMap.keySet()) {
            RuleConstructor constructor = new RuleConstructor(pred, pred2ArgSetMap);
            try {
                List<JplRule> rules = constructor.findRules();
                for (JplRule rule: rules) {
                    Validness validness = calValidness(rule);
                    System.out.printf(
                            "%d/%d(%f)\t%s\n", validness.posCnt, validness.negCnt,
                            validness.getValidness(), rule
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (debug) {
                break;
            }
        }
    }

    private Validness calValidness(JplRule rule) {
        /* Instanciate a rule by binding its variables to each possible constant */
        int pos_cnt = 0;
        int neg_cnt = 0;
        if (0 < rule.body.length) {
            String body_str = rule.getBodyString();
            Query q = new Query(":", new Term[]{
                    new Atom(PrologModule.GLOBAL.getSessionName()), Term.textToTerm(body_str)
            });
            for (Map<String, Term> binding: q) {
                Compound head_substituted = substitute(rule.head, binding);
                if (originalFacts.contains(head_substituted)) {
                    pos_cnt++;
                } else {
                    neg_cnt++;
                }
            }
        }
        return new Validness(pos_cnt, neg_cnt);
    }

    private Compound substitute(Compound compound, Map<String, Term> binding) {
        Term[] bounded_args = new Term[compound.arity()];
        for (int i = 0; i < bounded_args.length; i++) {
            Term original = compound.arg(i+1);
            bounded_args[i] = binding.getOrDefault(original.name(), original);
        }
        return new Compound(compound.name(), bounded_args);
    }

    public static void main(String[] args) {
        Compressor compressor = new Compressor(
                "FamilyRelationSimple(0.05)(100x).tsv",
                "none",
                true
        );
        compressor.observeFirstRoundRules();
    }
}
