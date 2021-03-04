package sinc.util.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackVertexSetSolverTest {
    static class GraphNodeWithName extends BaseGraphNode {
        String name;

        public GraphNodeWithName(String name) {
            this.name = name;
        }
    }
    @Test
    void test1() {
        /* 2 vertices cycle */
        Map<GraphNodeWithName, Set<GraphNodeWithName>> graph = new HashMap<>();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        graph.put(n1, new HashSet<>(Collections.singleton(n2)));
        graph.put(n2, new HashSet<>(Collections.singleton(n1)));

        Set<GraphNodeWithName> scc = new HashSet<>(Arrays.asList(n1, n2));

        FeedbackVertexSetSolver<GraphNodeWithName> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNodeWithName> cover = solver.run();

        assertEquals(1, cover.size());
        assertTrue(cover.contains(n1) || cover.contains(n2));
    }

    @Test
    void test2() {
        /* 4 vertices cycle(with redundancy) */
        Map<GraphNodeWithName, Set<GraphNodeWithName>> graph = new HashMap<>();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        GraphNodeWithName n4 = new GraphNodeWithName("n4");
        GraphNodeWithName n5 = new GraphNodeWithName("n5");
        GraphNodeWithName n6 = new GraphNodeWithName("n6");
        GraphNodeWithName n7 = new GraphNodeWithName("n7");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n5)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n4)));
        graph.put(n4, new HashSet<>(Collections.singletonList(n1)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n4)));
        graph.put(n7, new HashSet<>(Collections.singletonList(n4)));

        Set<GraphNodeWithName> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4));

        FeedbackVertexSetSolver<GraphNodeWithName> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNodeWithName> cover = solver.run();

        assertEquals(1, cover.size());
        assertTrue(cover.contains(n1) || cover.contains(n2) || cover.contains(n3) || cover.contains(n4));
    }

    @Test
    void test3() {
        /* 3 vertices cycle(with 2 self loops and redundancy) */
        Map<GraphNodeWithName, Set<GraphNodeWithName>> graph = new HashMap<>();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        GraphNodeWithName n4 = new GraphNodeWithName("n4");
        GraphNodeWithName n5 = new GraphNodeWithName("n5");
        GraphNodeWithName n6 = new GraphNodeWithName("n6");
        GraphNodeWithName n7 = new GraphNodeWithName("n7");
        graph.put(n1, new HashSet<>(Arrays.asList(n1, n2)));
        graph.put(n2, new HashSet<>(Arrays.asList(n2, n3)));
        graph.put(n3, new HashSet<>(Arrays.asList(n6, n1)));
        graph.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n7)));
        graph.put(n7, new HashSet<>(Collections.singletonList(n6)));

        Set<GraphNodeWithName> scc = new HashSet<>(Arrays.asList(n1, n2, n3));

        FeedbackVertexSetSolver<GraphNodeWithName> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNodeWithName> cover = solver.run();

        assertEquals(2, cover.size());
        assertTrue(cover.contains(n1));
        assertTrue(cover.contains(n2));
    }

    @Test
    void test4() {
        /* 6 vertices, 3 cycles */
        Map<GraphNodeWithName, Set<GraphNodeWithName>> graph = new HashMap<>();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        GraphNodeWithName n4 = new GraphNodeWithName("n4");
        GraphNodeWithName n5 = new GraphNodeWithName("n5");
        GraphNodeWithName n6 = new GraphNodeWithName("n6");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1, n4)));
        graph.put(n4, new HashSet<>(Arrays.asList(n2, n5)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n6)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n4)));

        Set<GraphNodeWithName> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4, n5, n6));

        FeedbackVertexSetSolver<GraphNodeWithName> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNodeWithName> cover = solver.run();

        assertEquals(2, cover.size());
        assertTrue(cover.contains(n4));
        assertTrue(cover.contains(n1) || cover.contains(n2) || cover.contains(n3));
    }

    @Test
    void test5() {
        /* 4 vertices complete graph */
        Map<GraphNodeWithName, Set<GraphNodeWithName>> graph = new HashMap<>();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        GraphNodeWithName n4 = new GraphNodeWithName("n4");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n3, n4)));
        graph.put(n2, new HashSet<>(Arrays.asList(n1, n3, n4)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1, n2, n4)));
        graph.put(n4, new HashSet<>(Arrays.asList(n1, n2, n3)));

        Set<GraphNodeWithName> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4));

        FeedbackVertexSetSolver<GraphNodeWithName> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNodeWithName> cover = solver.run();

        assertEquals(3, cover.size());
        assertEquals(3, (cover.contains(n1)?1:0) + (cover.contains(n2)?1:0) +
                (cover.contains(n3)?1:0) + (cover.contains(n4)?1:0));
    }
}