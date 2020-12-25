package utils.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TarjanTest {

    @Test
    public void testRun() {
        Map<BaseGraphNode, Set<BaseGraphNode>> graph = new HashMap<>();
        BaseGraphNode n1 = new BaseGraphNode();
        BaseGraphNode n2 = new BaseGraphNode();
        BaseGraphNode n3 = new BaseGraphNode();
        BaseGraphNode n4 = new BaseGraphNode();
        BaseGraphNode n5 = new BaseGraphNode();
        BaseGraphNode n6 = new BaseGraphNode();
        BaseGraphNode n7 = new BaseGraphNode();
        BaseGraphNode n8 = new BaseGraphNode();
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n4)));
        graph.put(n2, new HashSet<>(Arrays.asList(n3, n5)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1)));
        graph.put(n4, new HashSet<>(Arrays.asList(n3)));
        graph.put(n5, new HashSet<>(Arrays.asList(n6, n7)));
        graph.put(n6, new HashSet<>(Arrays.asList(n5)));
        graph.put(n7, new HashSet<>(Arrays.asList(n5)));

        Tarjan<BaseGraphNode> tarjan = new Tarjan<>(graph);
        List<List<BaseGraphNode>> sccs = tarjan.run();
        assertEquals(2, sccs.size());
        for (int i = 0; i < 2; i++) {
            List<BaseGraphNode> scc = sccs.get(i);
            switch (scc.size()) {
                case 3:
                    assertTrue(scc.contains(n5));
                    assertTrue(scc.contains(n6));
                    assertTrue(scc.contains(n6));
                    break;
                case 4:
                    assertTrue(scc.contains(n1));
                    assertTrue(scc.contains(n2));
                    assertTrue(scc.contains(n3));
                    assertTrue(scc.contains(n4));
                    break;
                default:
                    fail();
            }
        }
    }

    @Test
    public void testRun2() {
        BaseGraphNode n1 = new BaseGraphNode();
        BaseGraphNode n2 = new BaseGraphNode();
        BaseGraphNode n3 = new BaseGraphNode();
        BaseGraphNode n4 = new BaseGraphNode();
        BaseGraphNode n5 = new BaseGraphNode();
        BaseGraphNode n6 = new BaseGraphNode();
        BaseGraphNode n7 = new BaseGraphNode();
        BaseGraphNode n8 = new BaseGraphNode();
        BaseGraphNode n9 = new BaseGraphNode();
        BaseGraphNode n10 = new BaseGraphNode();
        BaseGraphNode n11 = new BaseGraphNode();
        BaseGraphNode n12 = new BaseGraphNode();
        BaseGraphNode n13 = new BaseGraphNode();
        BaseGraphNode n14 = new BaseGraphNode();
        BaseGraphNode n15 = new BaseGraphNode();
        BaseGraphNode n16 = new BaseGraphNode();
        BaseGraphNode n17 = new BaseGraphNode();
        BaseGraphNode n18 = new BaseGraphNode();
        BaseGraphNode n19 = new BaseGraphNode();
        BaseGraphNode n20 = new BaseGraphNode();
        BaseGraphNode n21 = new BaseGraphNode();
        BaseGraphNode n22 = new BaseGraphNode();
        BaseGraphNode n23 = new BaseGraphNode();
        BaseGraphNode n24 = new BaseGraphNode();

        Map<BaseGraphNode, Set<BaseGraphNode>> graph = new HashMap<>();
        graph.put(n1, new HashSet<>(Arrays.asList(n9, n15)));
        graph.put(n2, new HashSet<>(Arrays.asList(n9, n16)));
        graph.put(n3, new HashSet<>(Arrays.asList(n10, n17)));
        graph.put(n4, new HashSet<>(Arrays.asList(n11, n18)));
        graph.put(n5, new HashSet<>(Arrays.asList(n12, n19)));
        graph.put(n6, new HashSet<>(Arrays.asList(n12, n20)));
        graph.put(n7, new HashSet<>(Arrays.asList(n13, n21)));
        graph.put(n8, new HashSet<>(Arrays.asList(n14, n22)));

        Tarjan<BaseGraphNode> tarjan = new Tarjan<>(graph);
        List<List<BaseGraphNode>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }

    @Test
    public void testRun3() {
        BaseGraphNode n1 = new BaseGraphNode();
        BaseGraphNode n2 = new BaseGraphNode();
        BaseGraphNode n3 = new BaseGraphNode();

        Map<BaseGraphNode, Set<BaseGraphNode>> graph = new HashMap<>();
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n3)));

        Tarjan<BaseGraphNode> tarjan = new Tarjan<>(graph);
        List<List<BaseGraphNode>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }
}