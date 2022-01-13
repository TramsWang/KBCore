package iknows.util.amie;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AmieRuleMonitor {
    int totalRules = 0;
    int usedRules = 0;
    List<Integer> totalRulesLengthDistribution = new ArrayList<>();
    List<Integer> usedRulesLengthDistribution = new ArrayList<>();

    public void show(PrintWriter writer) {
        writer.println("### AMIE Monitored Info ###\n");
        writer.println("--- Statistics ---");
        writer.printf(
                "# %10s %10s %10s\n",
                "Total #H", "Used #H", "Usage(%)"
        );
        writer.printf(
                "  %10d %10d %10.2f\n",
                totalRules,
                usedRules,
                usedRules * 100.0 / totalRules
        );
        writer.print("- Total Rule Length Distribution: ");
        writer.println(Arrays.toString(totalRulesLengthDistribution.toArray(new Integer[0])));
        writer.print("- Used Rule Length Distribution: ");
        writer.println(Arrays.toString(usedRulesLengthDistribution.toArray(new Integer[0])));
        writer.println();
    }
}
