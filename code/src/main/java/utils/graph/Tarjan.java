package utils.graph;

import java.util.*;

public class Tarjan<T extends BaseGraphNode> {
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

        for (T neighbour : graph.get(node)) {
            if (-1 == neighbour.index) {
                strongConnect(neighbour);
                node.lowLink = Math.min(node.lowLink, neighbour.lowLink);
            } else if (neighbour.onStack) {
                node.lowLink = Math.min(node.lowLink, neighbour.index);
            }
        }

        if (node.lowLink == node.index) {
            /* 只返回非平凡的强连通分量 */
            if (1 < stack.size()) {
                Set<T> scc = new HashSet<>(stack);
                result.add(scc);
            }
            for (T n : stack) {
                n.onStack = false;
            }
            stack.clear();
        }
    }
}
