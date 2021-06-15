package sinc.impl.cached;

public class CachedQueryMonitor {
    private static final int DENOMINATOR = 1000000;

    public long preComputingCostInNano = 0;
    public long allEntailQueryCostInNano = 0;
    public long posEntailQueryCostInNano = 0;

    public long boundExistVarCostInNano = 0;
    public long boundExistVarInNewPredCostInNano = 0;
    public long boundNewVarCostInNano = 0;
    public long boundNewVarInNewPredCostInNano = 0;
    public long boundConstCostInNano = 0;

    public long cloneCostInNano = 0;
    public int totalClones = 0;

    public void show() {
        System.out.println("### Cached Query Monitored Info ###\n");
        System.out.println("--- Time Cost ---");
        System.out.printf(
                "T(ms) %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n",
                "[Pre", "AllEntail", "+Entail]", "[EBV", "EBV+", "NBV", "NBV+", "Const]", "#clone", "clone");
        System.out.printf(
                "      %10d %10d %10d %10d %10d %10d %10d %10d %10d %10d\n",
                preComputingCostInNano / DENOMINATOR,
                allEntailQueryCostInNano / DENOMINATOR,
                posEntailQueryCostInNano / DENOMINATOR,
                boundExistVarCostInNano / DENOMINATOR,
                boundExistVarInNewPredCostInNano / DENOMINATOR,
                boundNewVarCostInNano / DENOMINATOR,
                boundNewVarInNewPredCostInNano / DENOMINATOR,
                boundConstCostInNano / DENOMINATOR,
                totalClones,
                cloneCostInNano / DENOMINATOR
        );
    }
}
