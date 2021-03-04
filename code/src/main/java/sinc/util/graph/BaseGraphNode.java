package sinc.util.graph;

public class BaseGraphNode {
    public static final int NO_TARJAN_INDEX = -1;
    public static final int NO_TARJAN_LOW_LINK = -1;
    public static final int NO_FVS_INDEX = -1;

    /* parameters for Tarjan */
    public int index = NO_TARJAN_INDEX;
    public int lowLink = NO_TARJAN_LOW_LINK;
    public boolean onStack = false;

    /* parameters for fvs */
    public int fvsIdx = NO_FVS_INDEX;
}
