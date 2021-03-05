package sinc.util.graph;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

public class FeedbackVertexSetSolver<T extends BaseGraphNode> {

    private final List<T> nodes;
    private final INDArray matrix;
    private final int size;

    public FeedbackVertexSetSolver(Map<T, Set<T>> graph, Set<T> scc) {
        /* 在这里把SCC转成邻接矩阵的形式 */
        size = scc.size();
        matrix = Nd4j.zeros(size, size);

        /* 先把每个点编号 */
        nodes = new ArrayList<>(size);
        for (T node: scc) {
            node.fvsIdx = nodes.size();
            nodes.add(node);
        }

        /* 截取graph中属于SCC的部分 */
        for (T node: nodes) {
            Set<T> successors = graph.get(node);
            for (T successor: successors) {
                if (T.NO_FVS_INDEX != successor.fvsIdx) {
                    matrix.putScalar(new int[]{node.fvsIdx, successor.fvsIdx}, 1);
                }
            }
        }

        /* 将节点的编号取消，否则处理同一张图的其他scc时可能会引起混乱 */
        for (T node: nodes) {
            node.fvsIdx = T.NO_FVS_INDEX;
        }
    }

    public Set<T> run() {
        /* 每次取|in|x|out|最大的点，然后把相关的环删掉，直到最后没有环 */
        Set<T> result = new HashSet<>();
        while (0 < matrix.sum(0, 1).getInt(0)) {
            INDArray out_edges = matrix.sum(1);
            INDArray in_edges = matrix.sum(0);
            INDArray score = out_edges.mul(in_edges);
            int max_idx = score.argMax(0).getInt(0);
            result.add(nodes.get(max_idx));

            /* 从SCC中删除这个点以及相关的环 */
            matrix.putRow(max_idx, Nd4j.zeros(size));
            matrix.putColumn(max_idx, Nd4j.zeros(size));
            boolean updated = true;
            while (updated) {
                updated = false;
                out_edges = matrix.sum(1);
                in_edges = matrix.sum(0);
                for (int i = 0; i < size; i++) {
                    boolean has_out_edge = out_edges.getInt(i) > 0;
                    boolean has_in_edge = in_edges.getInt(i) > 0;
                    if (has_out_edge ^ has_in_edge) {
                        matrix.putRow(i, Nd4j.zeros(size));
                        matrix.putColumn(i, Nd4j.zeros(size));
                        updated = true;
                    }
                }
            }
        }
        return result;
    }
}
