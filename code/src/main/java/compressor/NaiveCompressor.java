package compressor;

import common.GraphNode4Compound;
import common.JplRule;
import org.jpl7.*;
import utils.AmieRuleLoader;
import utils.graph.GraphView;
import utils.graph.Tarjan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class NaiveCompressor {
    private enum PrologModule {
        COMPLETE_BK("complete_bk"), START_SET("start_set");

        private final String sessionName;

        PrologModule(String sessionName) {
            this.sessionName = sessionName;
        }

        public String getSessionName() {
            return sessionName;
        }
    }

    private final String bkPath;
    private final String hypothesisPath;
    private final boolean debug;

    private final Set<Compound> facts = new HashSet<>();  // <fact, functor>
    private final Set<String> predicates = new HashSet<>();
    private final List<JplRule> rules = new ArrayList<>();
    private final Map<GraphNode4Compound, Set<GraphNode4Compound>> graph = new HashMap<>();
    private final Set<Compound> counterExamples = new HashSet<>();
    private final Set<Compound> startSet = new HashSet<>();

    public NaiveCompressor(String bkPath, String hypothesisPath, boolean debug) {
        this.bkPath = bkPath;
        this.hypothesisPath = hypothesisPath;
        this.debug = debug;
    }

    public void run() {
        JPL.init();
        long time_start = System.currentTimeMillis();
        loadBk();
        long time_bk_loaded = System.currentTimeMillis();
        System.out.printf("Background Knowledge Loaded in %fs\n", (time_bk_loaded - time_start) / 1000.0);

        loadHypothesis();
        long time_hypothesis_loaded = System.currentTimeMillis();
        System.out.printf("Hypothesis Loaded in %fs\n", (time_hypothesis_loaded - time_bk_loaded) / 1000.0);

        constructGraph();
        long time_graph_constructed = System.currentTimeMillis();
        System.out.printf("Graph Constructed in %fs\n", (time_graph_constructed - time_hypothesis_loaded) / 1000.0);

        findCore();
        long time_core_found = System.currentTimeMillis();
        System.out.printf("Core found in %fs\n", (time_core_found - time_graph_constructed) / 1000.0);
        System.out.printf("Total Time: %fs\n", (time_core_found - time_start) / 1000.0);
    }

    public void runAndValidate() {
        JPL.init();
        long time_start = System.currentTimeMillis();
        loadBk();
        long time_bk_loaded = System.currentTimeMillis();
        System.out.printf("Background Knowledge Loaded in %fs\n", (time_bk_loaded - time_start) / 1000.0);

        loadHypothesis();
        long time_hypothesis_loaded = System.currentTimeMillis();
        System.out.printf("Hypothesis Loaded in %fs\n", (time_hypothesis_loaded - time_bk_loaded) / 1000.0);

        constructGraph();
        long time_graph_constructed = System.currentTimeMillis();
        System.out.printf("Graph Constructed in %fs\n", (time_graph_constructed - time_hypothesis_loaded) / 1000.0);

        findCore();
        long time_core_found = System.currentTimeMillis();
        System.out.printf("Core found in %fs\n", (time_core_found - time_graph_constructed) / 1000.0);
        System.out.printf("Total Time: %fs\n", (time_core_found - time_start) / 1000.0);

        validate();
        long time_validation_finished = System.currentTimeMillis();
        System.out.printf("[Validation finished in %fs]\n", (time_validation_finished - time_core_found) / 1000.);
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

    private void loadBk() {
        try {
            System.out.println("\n>>> Loading BK...");
            BufferedReader reader = new BufferedReader(new FileReader(bkPath));
            String line;
            while (null != (line = reader.readLine())) {
                String[] components = line.split("\t");
                String subj = components[0];
                String pred = components[1];
                String obj = components[2];
                Compound fact = new Compound(pred, new Term[]{new Atom(subj), new Atom(obj)});
                facts.add(fact);
                predicates.add(pred);
                appendKnowledge(PrologModule.COMPLETE_BK, fact);
            }
            System.out.printf("BK loaded: %d predicates; %d facts\n", predicates.size(), facts.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHypothesis() {
        try {
            System.out.println("\n>>> Loading Hypothesis...");
            BufferedReader reader = new BufferedReader(new FileReader(hypothesisPath));
            String line;
            while (null != (line = reader.readLine())) {
                rules.add(AmieRuleLoader.toPrologSyntaxObject(line));
            }
            System.out.printf("Hypothesis loaded: %d rules\n", rules.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void constructGraph() {
        System.out.println("\n>>> Constructing Graph...");
        for (int i = 0; i < rules.size(); i++) {
            System.out.printf("Instanciating rule %d/%d...\n", i + 1, rules.size());
            JplRule rule = rules.get(i);

            /* Instanciate a rule by binding its variables to each possible constant */
            StringBuilder builder = new StringBuilder();
            if (0 < rule.body.length) {
                builder.append(rule.body[0].toString());
                for (int j = 1; j < rule.body.length; j++) {
                    builder.append(',').append(rule.body[j].toString());
                }
                Query q = new Query(":", new Term[]{
                        new Atom(PrologModule.COMPLETE_BK.getSessionName()), Term.textToTerm(builder.toString())
                });
                for (Map<String, Term> binding: q) {
                    Compound head_substituted = substitute(rule.head, binding);
                    GraphNode4Compound head_node = new GraphNode4Compound(head_substituted);
                    if (facts.contains(head_substituted)) {
                        Set<GraphNode4Compound> neighbours = graph.computeIfAbsent(head_node, k -> new HashSet<>());
                        for (Compound body: rule.body) {
                            Compound body_substituted = substitute(body, binding);
                            GraphNode4Compound body_node = new GraphNode4Compound(body_substituted);
                            neighbours.add(body_node);
                        }
                    } else {
                        counterExamples.add(head_substituted);
                    }
                }
            }
        }

        /* Count Graph */
        int total_edges = 0;
        for (Set<GraphNode4Compound> neighbours: graph.values()) {
            total_edges += neighbours.size();
        }
        System.out.printf("Graph Constructed: %d edges\n", total_edges);
    }

    private Compound substitute(Compound compound, Map<String, Term> binding) {
        Term[] bounded_args = new Term[compound.arity()];
        for (int i = 0; i < bounded_args.length; i++) {
            Term original = compound.arg(i+1);
            bounded_args[i] = binding.getOrDefault(original.name(), original);
        }
        return new Compound(compound.name(), bounded_args);
    }

    private void findCore() {
        System.out.println("\n>>> Finding Core...");
        /* 先把所有没有依赖（出边）的点加入START set */
        for (Compound fact: facts) {
            GraphNode4Compound fact_node = new GraphNode4Compound(fact);
            if (!graph.containsKey(fact_node)) {
                startSet.add(fact);
            }
        }

        /* 再分析图上的所有强连通分量，把每一个强连通分量中的点都加入START set */
        final int old_start_set_size = startSet.size();
        Tarjan<GraphNode4Compound> tarjan = new Tarjan<>(graph);
        List<List<GraphNode4Compound>> sccs = tarjan.run();
        for (List<GraphNode4Compound> scc: sccs) {
            for (GraphNode4Compound node: scc) {
                startSet.add(node.compound);
            }
        }

        System.out.printf(
                "Core Found: %d in START set(%d SCCs; without SCC: %d); %d in COUNTER EXAMPLE set\n",
                startSet.size(), sccs.size(), old_start_set_size, counterExamples.size()
        );
    }

    private void validate() {
        System.out.println("\n>>> Validating...");

        /* Debug: View the Graph */
        if (debug) {
            System.out.print("[DEBUG] Drawing Graph on Neo4j...");
            Set<Compound> nodes = new HashSet<>(facts);
            List<GraphView.Edge> edges = new ArrayList<>();
            for (Map.Entry<GraphNode4Compound, Set<GraphNode4Compound>> entry: graph.entrySet()) {
                nodes.add(entry.getKey().compound);
                for (GraphNode4Compound neighbour: entry.getValue()) {
                    edges.add(new GraphView.Edge(entry.getKey().compound, neighbour.compound));
                    nodes.add(neighbour.compound);
                }
            }
            GraphView graph_view = new GraphView();
            graph_view.draw(nodes, edges);
            System.out.println("Done");
        }

        /* Declare all predicates in START_SET module */
        for (String predicate: predicates) {
            Query q = new Query(Term.textToTerm(
                    String.format("dynamic %s:%s/2", PrologModule.START_SET.getSessionName(), predicate)
            ));
            q.hasSolution();
            q.close();
        }

        /* Add knowledge to START_SET module */
        for (Compound compound: startSet) {
            appendKnowledge(PrologModule.START_SET, compound);
        }

        /* Add Rules to START_SET module */
        for (JplRule rule: rules) {
            appendKnowledge(PrologModule.START_SET, Term.textToTerm(rule.toString()));
        }

        /* Check all facts in GOAL set(for those that cannot be proved, add to START set) */
        for (Compound goal: facts) {
            if (!startSet.contains(goal)) {
                try {
                    Query q = new Query(new Compound(":", new Term[]{
                            new Atom(PrologModule.START_SET.getSessionName()), goal
                    }));
//            System.out.println(q.toString());
                    if (!q.hasSolution()) {
                        System.err.printf("Goal not provable: %s\n", goal.toString());
                    }
                    q.close();
                } catch (PrologException e) {
                    e.printStackTrace();
                    System.err.printf("Unsolved Goal: %s\n", goal.toString());
                }
            }
        }

        System.out.println("Validation Finished");
    }

    public static void main(String[] args) {
//        NaiveCompressor compressor = new NaiveCompressor("FamilyRelationSimple(0.000000)(1x).tsv", "HypothesisHumanSimple.amie");
        NaiveCompressor compressor = new NaiveCompressor(
                "FamilyRelationMedium(0.200000)(10x).tsv",
                "HypothesisHumanMedium.amie",
                true
        );
        compressor.runAndValidate();

        try {
            PrintWriter writer = new PrintWriter("startSet.pl");
            for (Compound compound: compressor.startSet) {
                writer.print(compound.toString());
                writer.println('.');
            }
            writer.close();

            writer = new PrintWriter("counterExamples.pl");
            for (Compound compound: compressor.counterExamples) {
                writer.print(compound.toString());
                writer.println('.');
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
