package sinc.common;

import org.jpl7.Compound;
import sinc.util.graph.BaseGraphNode;

import java.util.Objects;

public class GraphNode4Compound extends BaseGraphNode {
    public final Compound compound;

    public GraphNode4Compound(Compound compound) {
        this.compound = compound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode4Compound graphNode = (GraphNode4Compound) o;
        return Objects.equals(compound, graphNode.compound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(compound);
    }
}
