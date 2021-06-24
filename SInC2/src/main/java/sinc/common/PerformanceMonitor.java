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
    public int kbFunctors = 0;
    public int kbConstants = 0;
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
    public int invalidSearches = 0;
    public int duplications = 0;
    public int hcFilteredRules = 0;
    public int totalConstantSubstitutions = 0;
    public int actualConstantSubstitutions = 0;
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
                "# %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n",
                "#F", "|C|", "|B|", "#H", "|H|", "|N|", "|A|", "|N/FVS|", "#SCC", "|SCC|", "|FVS|", "Comp(%)"
        );
        System.out.printf(
                "  %10d %10d %10d %10d %10d %10d %10d %10d %10d %10d %10d %10.2f\n\n",
                kbFunctors,
                kbConstants,
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
        int max_branches = 0;
        int max_rule_size = 0;
        int total_rule_size = 0;
        int max_ext = 0;
        int total_ext = 0;
        int max_org = 0;
        int total_org = 0;
        int[] rule_size_arr = new int[branchProgress.size()];
        int[] ext_num_arr = new int[branchProgress.size()];
        int[] org_num_arr = new int[branchProgress.size()];
        for (int i = 0; i < branchProgress.size(); i++) {
            BranchInfo branches = branchProgress.get(i);
            rule_size_arr[i] = branches.ruleSize;
            ext_num_arr[i] = branches.extNum;
            org_num_arr[i] = branches.orgNum;

            final int cur_branches = branches.extNum + branches.orgNum;
            executed_evaluations += cur_branches;
            max_branches = Math.max(cur_branches, max_branches);
            max_rule_size = Math.max(max_rule_size, branches.ruleSize);
            total_rule_size += branches.ruleSize;
            max_ext = Math.max(max_ext, branches.extNum);
            total_ext += branches.extNum;
            max_org = Math.max(max_org, branches.orgNum);
            total_org += branches.orgNum;
        }
        System.out.printf(
                "# %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n",
                "#Invalid", "#Dup", "#Eval", "#+Subs", "#Subs", "max(Brh)", "avg(Brh)",
                "max(|r|)", "avg(|r|)", "max(Ext)", "avg(Ext)", "max(Org)", "avg(Org)"
        );
        System.out.printf(
                "  %10d %10d %10d %10d %10d %10d %10.2f %10d %10.2f %10d %10.2f %10d %10.2f\n\n",
                invalidSearches,
                duplications,
                executed_evaluations,
                actualConstantSubstitutions,
                totalConstantSubstitutions,
                max_branches,
                (double) executed_evaluations / branchProgress.size(),
                max_rule_size,
                (double) total_rule_size / branchProgress.size(),
                max_ext,
                (double) total_ext / branchProgress.size(),
                max_org,
                (double) total_org / branchProgress.size()
        );
        System.out.print("- Rule Sizes: ");
        System.out.println(Arrays.toString(rule_size_arr));
        System.out.print("- Extensions: ");
        System.out.println(Arrays.toString(ext_num_arr));
        System.out.print("- Origins: ");
        System.out.println(Arrays.toString(org_num_arr));
        System.out.println();
    }
}
