package sinc.util.graph;

import java.util.*;

public class Tarjan<T extends BaseGraphNode<?>> {
    private int index = 0;
    private final Stack<T> stack = new Stack<>();
    private final List<Set<T>> result = new ArrayList<>();
    private final Map<T, Set<T>> graph;

    public Tarjan(Map<T, Set<T>> graph) {
        this.graph = graph;
    }

    public List<Set<T>> run() {
        for (T node : graph.keySet()) {
            if (-1 == node.index) {
                strongConnect(node);
            }
        }
        return result;
    }

    private void strongConnect(T node) {
        node.index = index;
        node.lowLink = index;
        node.onStack = true;
        index++;
        stack.push(node);

        Set<T> neighbours = graph.get(node);
        if (null != neighbours) {
            for (T neighbour : neighbours) {
                if (-1 == neighbour.index) {
                    strongConnect(neighbour);
                    node.lowLink = Math.min(node.lowLink, neighbour.lowLink);
                } else if (neighbour.onStack) {
                    node.lowLink = Math.min(node.lowLink, neighbour.index);
                }
            }
        }

        if (node.lowLink == node.index) {
            Set<T> scc = new HashSet<>();
            T top;
            do {
                top = stack.pop();
                top.onStack = false;
                scc.add(top);
            } while (!node.equals(top));

            /* 只返回非平凡的强连通分量 */
            if (1 < scc.size()) {
                result.add(scc);
            }
        }
    }
}
