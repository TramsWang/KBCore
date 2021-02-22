package sinc.util.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeedbackVertexSet<T extends BaseGraphNode> {

    private final Map<T, Set<T>> graph;

    public FeedbackVertexSet(Map<T, Set<T>> graph) {
        this.graph = graph;
    }

    public List<T> run() {
        // Todo: Implement Here
        return null;
    }
}
