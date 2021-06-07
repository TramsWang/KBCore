package sinc.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerformanceMonitor {
    public static class BranchInfo {
        int ruleSize;
        int extNum;
        int orgNum;

        public BranchInfo(int ruleSize, int extNum, int orgNum) {
            this.ruleSize = ruleSize;
            this.extNum = extNum;
            this.orgNum = orgNum;
        }
    }

    /* Time Monitor */
    public long kbLoadTime = 0;
    public long hypothesisMiningTime = 0;
    public long otherMiningTime = 0;  // Counter Examples + Start Set(Dependency Graph)
    public long validationTime = 0;
    public long dumpTime = 0;
    public long totalTime = 0;

    /* Mining Statics Monitor */
    public int kbSize = 0;
    public int hypothesisRuleNumber = 0;
    public int hypothesisSize = 0;
    public int startSetSize = 0;
    public int startSetSizeWithoutFvs = 0;
    public int counterExampleSize = 0;
    public int sccNumber = 0;
    public int sccVertices = 0;
    public int fvsVertices = 0;

    /* Other Statistics Monitor */
    public int cacheHits = 0;
    public List<BranchInfo> branchProgress = new ArrayList<>();

    public void show() {
        System.out.println("### Monitored Performance Info ###\n");
        System.out.println("--- Time Cost ---");
        System.out.printf(
                "(ms) %10s %10s %10s %10s %10s %10s\n",
                "Load", "Hypo", "N&A", "Validate", "Dump", "Total"
        );
        System.out.printf(
                "     %10d %10d %10d %10d %10d %10d\n\n",
                kbLoadTime, hypothesisMiningTime, otherMiningTime, validationTime, dumpTime, totalTime
        );

        System.out.println("--- Statistics ---");
        System.out.printf(
                "# %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n",
                "|B|", "#H", "|H|", "|N|", "|A|", "|N/FVS|", "#SCC", "|SCC|", "|FVS|", "Comp(%)"
        );
        System.out.printf(
                "  %10d %10d %10d %10d %10d %10d %10d %10d %10d %10.2f\n\n",
                kbSize,
                hypothesisRuleNumber,
                hypothesisSize,
                startSetSize,
                counterExampleSize,
                startSetSizeWithoutFvs,
                sccNumber,
                sccVertices,
                fvsVertices,
                (startSetSize + counterExampleSize + hypothesisSize) * 100.0 / kbSize
        );

        System.out.println("--- Other Statistics ---");
        int executed_evaluations = 0;
        for (BranchInfo branches: branchProgress) {
            executed_evaluations += branches.extNum + branches.orgNum;
        }
        System.out.printf(
                "Cache Hits: %.2f%%(%d/%d)\n",
                cacheHits * 100.0 / (executed_evaluations + cacheHits),
                cacheHits,
                executed_evaluations + cacheHits
        );
        System.out.println("Branch Progress:");
        System.out.println(Arrays.toString(branchProgress.toArray(new BranchInfo[0])));
    }
}
