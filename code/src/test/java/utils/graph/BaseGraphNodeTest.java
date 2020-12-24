package utils.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseGraphNodeTest {
    @Test
    public void testEquality() {
        BaseGraphNode n1 = new BaseGraphNode();
        BaseGraphNode n2 = new BaseGraphNode();
        assertNotEquals(n1, n2);
    }
}