package sinc.util.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TarjanTest {
    static class GraphNodeWithName extends BaseGraphNode {
        String name;

        public GraphNodeWithName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class MapWithAppointedKeySet implements Map<GraphNodeWithName, Set<GraphNodeWithName>> {
        private final Map<GraphNodeWithName, Set<GraphNodeWithName>> actualMap = new HashMap<>();
        private final Set<GraphNodeWithName> appointedKeySet = new HashSet<>();
//        private final Set<Entry<GraphNodeWithName, Set<GraphNodeWithName>>> appointedEntrySet = new HashSet<>();

        public void addAppointedKey(GraphNodeWithName key) {
            appointedKeySet.add(key);
        }

        @Override
        public int size() {
            return actualMap.size();
        }

        @Override
        public Set<GraphNodeWithName> get(Object o) {
            return actualMap.get(o);
        }

        @Override
        public Set<GraphNodeWithName> put(GraphNodeWithName key, Set<GraphNodeWithName> value) {
            return actualMap.put(key, value);
        }

        @Override
        public boolean containsKey(Object o) {
            return actualMap.containsKey(o);
        }

        @Override
        public boolean containsValue(Object o) {
            return actualMap.containsValue(o);
        }

        @Override
        public Collection<Set<GraphNodeWithName>> values() {
            return actualMap.values();
        }

        @Override
        public boolean isEmpty() {
            return actualMap.isEmpty();
        }

        @Override
        public Set<Entry<GraphNodeWithName, Set<GraphNodeWithName>>> entrySet() {
            Set<Entry<GraphNodeWithName, Set<GraphNodeWithName>>> entry_set = new HashSet<>();
            for (Entry<GraphNodeWithName, Set<GraphNodeWithName>> entry: actualMap.entrySet()) {
                if (appointedKeySet.contains(entry.getKey())) {
                    entry_set.add(entry);
                }
            }
            return entry_set;
        }

        @Override
        public Set<GraphNodeWithName> keySet() {
            return appointedKeySet;
        }

        @Override
        public Set<GraphNodeWithName> remove(Object o) {
            return actualMap.remove(o);
        }

        @Override
        public void clear() {
            actualMap.clear();
            appointedKeySet.clear();
        }

        @Override
        public void putAll(Map<? extends GraphNodeWithName, ? extends Set<GraphNodeWithName>> map) {
            actualMap.putAll(map);
        }
    }

    @Test
    public void testAppointedMap() {
        MapWithAppointedKeySet map = new MapWithAppointedKeySet();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        map.put(n1, new HashSet<>(Collections.singletonList(n2)));
        map.put(n2, new HashSet<>(Collections.singletonList(n3)));
        map.addAppointedKey(n2);

        assertEquals(1, map.entrySet().size());
        for (Map.Entry<GraphNodeWithName, Set<GraphNodeWithName>> entry: map.entrySet()) {
            assertEquals(n2, entry.getKey());
            assertEquals(new HashSet<GraphNodeWithName>(Collections.singletonList(n3)), entry.getValue());
        }

        assertEquals(1, map.keySet().size());
        for (GraphNodeWithName node: map.keySet()) {
            assertEquals(n2, node);
        }
    }
    
    @Test
    public void testRun() {
        Map<BaseGraphNode, Set<BaseGraphNode>> graph = new HashMap<>();
        BaseGraphNode n1 = new GraphNodeWithName("n1");
        BaseGraphNode n2 = new GraphNodeWithName("n2");
        BaseGraphNode n3 = new GraphNodeWithName("n3");
        BaseGraphNode n4 = new GraphNodeWithName("n4");
        BaseGraphNode n5 = new GraphNodeWithName("n5");
        BaseGraphNode n6 = new GraphNodeWithName("n6");
        BaseGraphNode n7 = new GraphNodeWithName("n7");
        BaseGraphNode n8 = new GraphNodeWithName("n8");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n4)));
        graph.put(n2, new HashSet<>(Arrays.asList(n3, n5)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1)));
        graph.put(n4, new HashSet<>(Arrays.asList(n3)));
        graph.put(n5, new HashSet<>(Arrays.asList(n6, n7)));
        graph.put(n6, new HashSet<>(Arrays.asList(n5)));
        graph.put(n7, new HashSet<>(Arrays.asList(n5)));

        Tarjan<BaseGraphNode> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode>> sccs = tarjan.run();
        for (Set<BaseGraphNode> scc: sccs) {
            System.out.print("SCC: ");
            for (BaseGraphNode n: scc) {
                System.out.print(n + ", ");
            }
            System.out.println();
        }
        assertEquals(2, sccs.size());
        for (int i = 0; i < 2; i++) {
            Set<BaseGraphNode> scc = sccs.get(i);
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
        BaseGraphNode n1 = new GraphNodeWithName("n1");
        BaseGraphNode n2 = new GraphNodeWithName("n2");
        BaseGraphNode n3 = new GraphNodeWithName("n3");
        BaseGraphNode n4 = new GraphNodeWithName("n4");
        BaseGraphNode n5 = new GraphNodeWithName("n5");
        BaseGraphNode n6 = new GraphNodeWithName("n6");
        BaseGraphNode n7 = new GraphNodeWithName("n7");
        BaseGraphNode n8 = new GraphNodeWithName("n8");
        BaseGraphNode n9 = new GraphNodeWithName("n9");
        BaseGraphNode n10 = new GraphNodeWithName("n10");
        BaseGraphNode n11 = new GraphNodeWithName("n11");
        BaseGraphNode n12 = new GraphNodeWithName("n12");
        BaseGraphNode n13 = new GraphNodeWithName("n13");
        BaseGraphNode n14 = new GraphNodeWithName("n14");
        BaseGraphNode n15 = new GraphNodeWithName("n15");
        BaseGraphNode n16 = new GraphNodeWithName("n16");
        BaseGraphNode n17 = new GraphNodeWithName("n17");
        BaseGraphNode n18 = new GraphNodeWithName("n18");
        BaseGraphNode n19 = new GraphNodeWithName("n19");
        BaseGraphNode n20 = new GraphNodeWithName("n20");
        BaseGraphNode n21 = new GraphNodeWithName("n21");
        BaseGraphNode n22 = new GraphNodeWithName("n22");
        BaseGraphNode n23 = new GraphNodeWithName("n23");
        BaseGraphNode n24 = new GraphNodeWithName("n24");

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
        List<Set<BaseGraphNode>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }

    @Test
    public void testRun3() {
        BaseGraphNode n1 = new GraphNodeWithName("n1");
        BaseGraphNode n2 = new GraphNodeWithName("n2");
        BaseGraphNode n3 = new GraphNodeWithName("n3");

        Map<BaseGraphNode, Set<BaseGraphNode>> graph = new HashMap<>();
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n3)));

        Tarjan<BaseGraphNode> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }

    @Test
    public void testAppointedStartPoints1() {
        MapWithAppointedKeySet graph = new MapWithAppointedKeySet();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n2)));
        graph.addAppointedKey(n1);

        Tarjan<GraphNodeWithName> tarjan = new Tarjan<>(graph);
        List<Set<GraphNodeWithName>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3))));
    }

    @Test
    public void testAppointedStartPoints2() {
        MapWithAppointedKeySet graph = new MapWithAppointedKeySet();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n2)));
        graph.addAppointedKey(n2);

        Tarjan<GraphNodeWithName> tarjan = new Tarjan<>(graph);
        List<Set<GraphNodeWithName>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3))));
    }

    @Test
    public void testAppointedStartPoints3() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        GraphNodeWithName n4 = new GraphNodeWithName("n4");
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(Arrays.asList(n2, n4)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);

        Tarjan<GraphNodeWithName> tarjan = new Tarjan<>(graph1);
        List<Set<GraphNodeWithName>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3, n4))));
    }

    @Test
    public void testAppointedStartPoints4() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        GraphNodeWithName n4 = new GraphNodeWithName("n4");
        GraphNodeWithName n5 = new GraphNodeWithName("n5");
        GraphNodeWithName n6 = new GraphNodeWithName("n6");
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(Arrays.asList(n2, n4, n5, n6)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);

        Tarjan<GraphNodeWithName> tarjan = new Tarjan<>(graph1);
        List<Set<GraphNodeWithName>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3, n4))));
    }

    @Test
    public void testAppointedStartPoints5() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        GraphNodeWithName n0 = new GraphNodeWithName("n0");
        GraphNodeWithName n1 = new GraphNodeWithName("n1");
        GraphNodeWithName n2 = new GraphNodeWithName("n2");
        GraphNodeWithName n3 = new GraphNodeWithName("n3");
        GraphNodeWithName n4 = new GraphNodeWithName("n4");
        GraphNodeWithName n5 = new GraphNodeWithName("n5");
        GraphNodeWithName n6 = new GraphNodeWithName("n6");
        graph1.put(n0, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(Arrays.asList(n2, n4, n5, n6)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);
        graph1.addAppointedKey(n0);

        Tarjan<GraphNodeWithName> tarjan = new Tarjan<>(graph1);
        List<Set<GraphNodeWithName>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3, n4))));
    }
}