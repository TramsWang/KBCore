package sinc.util.graph;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

public class FeedbackVertexSetSolver<T extends BaseGraphNode> {

    private final List<T> nodes;
    private final INDArray matrix;

    public FeedbackVertexSetSolver(Map<T, Set<T>> graph, Set<T> scc) {
        /* 在这里把SCC转成邻接矩阵的形式 */
        matrix = Nd4j.zeros(scc.size(), scc.size());

        /* 先把每个点编号 */
        nodes = new ArrayList<>();
        for (T node: scc) {
            node.index = nodes.size();
            nodes.add(node);
        }

        /* 截取graph中属于SCC的部分 */
        for (T node: scc) {
            Set<T> successors = graph.get(node);
            for (T successor: successors) {
                if (T.NO_FVS_INDEX != successor.fvsIdx) {
                    matrix.putScalar(new int[]{node.fvsIdx, successor.fvsIdx}, 1);
                }
            }
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
            /* 从SCC中删除这个点 */
            matrix.putRow(max_idx, Nd4j.zeros(matrix.shape()[1]));
            matrix.putColumn(max_idx, Nd4j.zeros(matrix.shape()[0]));
        }
        return result;
    }
}
