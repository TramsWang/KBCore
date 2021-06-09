package sinc.util.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TarjanTest {

    static class MapWithAppointedKeySet implements Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> {
        private final Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> actualMap = new HashMap<>();
        private final Set<BaseGraphNode<String>> appointedKeySet = new HashSet<>();
//        private final Set<Entry<BaseGraphNode<String>, Set<BaseGraphNode<String>>>> appointedEntrySet = new HashSet<>();

        public void addAppointedKey(BaseGraphNode<String> key) {
            appointedKeySet.add(key);
        }

        @Override
        public int size() {
            return actualMap.size();
        }

        @Override
        public Set<BaseGraphNode<String>> get(Object o) {
            return actualMap.get(o);
        }

        @Override
        public Set<BaseGraphNode<String>> put(BaseGraphNode<String> key, Set<BaseGraphNode<String>> value) {
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
        public Collection<Set<BaseGraphNode<String>>> values() {
            return actualMap.values();
        }

        @Override
        public boolean isEmpty() {
            return actualMap.isEmpty();
        }

        @Override
        public Set<Entry<BaseGraphNode<String>, Set<BaseGraphNode<String>>>> entrySet() {
            Set<Entry<BaseGraphNode<String>, Set<BaseGraphNode<String>>>> entry_set = new HashSet<>();
            for (Entry<BaseGraphNode<String>, Set<BaseGraphNode<String>>> entry: actualMap.entrySet()) {
                if (appointedKeySet.contains(entry.getKey())) {
                    entry_set.add(entry);
                }
            }
            return entry_set;
        }

        @Override
        public Set<BaseGraphNode<String>> keySet() {
            return appointedKeySet;
        }

        @Override
        public Set<BaseGraphNode<String>> remove(Object o) {
            return actualMap.remove(o);
        }

        @Override
        public void clear() {
            actualMap.clear();
            appointedKeySet.clear();
        }

        @Override
        public void putAll(Map<? extends BaseGraphNode<String>, ? extends Set<BaseGraphNode<String>>> map) {
            actualMap.putAll(map);
        }
    }

    @Test
    public void testAppointedMap() {
        MapWithAppointedKeySet map = new MapWithAppointedKeySet();
        BaseGraphNode<String> n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<String>("n3");
        map.put(n1, new HashSet<>(Collections.singletonList(n2)));
        map.put(n2, new HashSet<>(Collections.singletonList(n3)));
        map.addAppointedKey(n2);

        assertEquals(1, map.entrySet().size());
        for (Map.Entry<BaseGraphNode<String>, Set<BaseGraphNode<String>>> entry: map.entrySet()) {
            assertEquals(n2, entry.getKey());
            assertEquals(new HashSet<BaseGraphNode<String>>(Collections.singletonList(n3)), entry.getValue());
        }

        assertEquals(1, map.keySet().size());
        for (BaseGraphNode<String> node: map.keySet()) {
            assertEquals(n2, node);
        }
    }

    @Test
    public void testRun() {
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String>n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String>n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String>n3 = new BaseGraphNode<String>("n3");
        BaseGraphNode<String>n4 = new BaseGraphNode<String>("n4");
        BaseGraphNode<String>n5 = new BaseGraphNode<String>("n5");
        BaseGraphNode<String>n6 = new BaseGraphNode<String>("n6");
        BaseGraphNode<String>n7 = new BaseGraphNode<String>("n7");
        BaseGraphNode<String>n8 = new BaseGraphNode<String>("n8");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n4)));
        graph.put(n2, new HashSet<>(Arrays.asList(n3, n5)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1)));
        graph.put(n4, new HashSet<>(Arrays.asList(n3)));
        graph.put(n5, new HashSet<>(Arrays.asList(n6, n7)));
        graph.put(n6, new HashSet<>(Arrays.asList(n5)));
        graph.put(n7, new HashSet<>(Arrays.asList(n5)));

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        for (Set<BaseGraphNode<String>> scc: sccs) {
            System.out.print("SCC: ");
            for (BaseGraphNode<String>n: scc) {
                System.out.print(n + ", ");
            }
            System.out.println();
        }
        assertEquals(2, sccs.size());
        for (int i = 0; i < 2; i++) {
            Set<BaseGraphNode<String>> scc = sccs.get(i);
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
        BaseGraphNode<String>n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String>n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String>n3 = new BaseGraphNode<String>("n3");
        BaseGraphNode<String>n4 = new BaseGraphNode<String>("n4");
        BaseGraphNode<String>n5 = new BaseGraphNode<String>("n5");
        BaseGraphNode<String>n6 = new BaseGraphNode<String>("n6");
        BaseGraphNode<String>n7 = new BaseGraphNode<String>("n7");
        BaseGraphNode<String>n8 = new BaseGraphNode<String>("n8");
        BaseGraphNode<String>n9 = new BaseGraphNode<String>("n9");
        BaseGraphNode<String>n10 = new BaseGraphNode<String>("n10");
        BaseGraphNode<String>n11 = new BaseGraphNode<String>("n11");
        BaseGraphNode<String>n12 = new BaseGraphNode<String>("n12");
        BaseGraphNode<String>n13 = new BaseGraphNode<String>("n13");
        BaseGraphNode<String>n14 = new BaseGraphNode<String>("n14");
        BaseGraphNode<String>n15 = new BaseGraphNode<String>("n15");
        BaseGraphNode<String>n16 = new BaseGraphNode<String>("n16");
        BaseGraphNode<String>n17 = new BaseGraphNode<String>("n17");
        BaseGraphNode<String>n18 = new BaseGraphNode<String>("n18");
        BaseGraphNode<String>n19 = new BaseGraphNode<String>("n19");
        BaseGraphNode<String>n20 = new BaseGraphNode<String>("n20");
        BaseGraphNode<String>n21 = new BaseGraphNode<String>("n21");
        BaseGraphNode<String>n22 = new BaseGraphNode<String>("n22");
        BaseGraphNode<String>n23 = new BaseGraphNode<String>("n23");
        BaseGraphNode<String>n24 = new BaseGraphNode<String>("n24");

        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        graph.put(n1, new HashSet<>(Arrays.asList(n9, n15)));
        graph.put(n2, new HashSet<>(Arrays.asList(n9, n16)));
        graph.put(n3, new HashSet<>(Arrays.asList(n10, n17)));
        graph.put(n4, new HashSet<>(Arrays.asList(n11, n18)));
        graph.put(n5, new HashSet<>(Arrays.asList(n12, n19)));
        graph.put(n6, new HashSet<>(Arrays.asList(n12, n20)));
        graph.put(n7, new HashSet<>(Arrays.asList(n13, n21)));
        graph.put(n8, new HashSet<>(Arrays.asList(n14, n22)));

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }

    @Test
    public void testRun3() {
        BaseGraphNode<String>n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String>n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String>n3 = new BaseGraphNode<String>("n3");

        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n3)));

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }

    @Test
    public void testAppointedStartPoints1() {
        MapWithAppointedKeySet graph = new MapWithAppointedKeySet();
        BaseGraphNode<String> n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<String>("n3");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n2)));
        graph.addAppointedKey(n1);

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3))));
    }

    @Test
    public void testAppointedStartPoints2() {
        MapWithAppointedKeySet graph = new MapWithAppointedKeySet();
        BaseGraphNode<String> n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<String>("n3");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n2)));
        graph.addAppointedKey(n2);

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3))));
    }

    @Test
    public void testAppointedStartPoints3() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        BaseGraphNode<String> n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<String>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<String>("n4");
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(Arrays.asList(n2, n4)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph1);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3, n4))));
    }

    @Test
    public void testAppointedStartPoints4() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        BaseGraphNode<String> n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<String>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<String>("n4");
        BaseGraphNode<String> n5 = new BaseGraphNode<String>("n5");
        BaseGraphNode<String> n6 = new BaseGraphNode<String>("n6");
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(Arrays.asList(n2, n4, n5, n6)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph1);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3, n4))));
    }

    @Test
    public void testAppointedStartPoints5() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        BaseGraphNode<String> n0 = new BaseGraphNode<String>("n0");
        BaseGraphNode<String> n1 = new BaseGraphNode<String>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<String>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<String>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<String>("n4");
        BaseGraphNode<String> n5 = new BaseGraphNode<String>("n5");
        BaseGraphNode<String> n6 = new BaseGraphNode<String>("n6");
        graph1.put(n0, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(Arrays.asList(n2, n4, n5, n6)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);
        graph1.addAppointedKey(n0);

        Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(graph1);
        List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(Arrays.asList(n2, n3, n4))));
    }
}