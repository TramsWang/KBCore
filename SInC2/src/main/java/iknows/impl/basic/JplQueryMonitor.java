package iknows.impl.basic;

public class JplQueryMonitor {
    private static final int DENOMINATOR = 1000000;

    public long preComputingCostInNano = 0;
    public long allEntailQueryCostInNano = 0;
    public long posEntailQueryCostInNano = 0;
    public long headTemplateBuildCostInNano = 0;
    public long jplQueryCostInNano = 0;
    public long substituteCostInNano = 0;

    public void show() {
        System.out.println("### Jpl Query Monitored Info ###\n");
        System.out.println("--- Time Cost ---");
        System.out.printf(
                "T(ms) %10s %10s %10s %15s %10s %10s\n",
                "[Pre", "AllEntail", "+Entail]", "[JplQuery", "HeadTmpl", "Subst]");
        System.out.printf(
                "      %10d %10d %10d %15d %10d %10d\n",
                preComputingCostInNano / DENOMINATOR,
                allEntailQueryCostInNano / DENOMINATOR,
                posEntailQueryCostInNano / DENOMINATOR,
                jplQueryCostInNano / DENOMINATOR,
                headTemplateBuildCostInNano / DENOMINATOR,
                substituteCostInNano / DENOMINATOR
        );
    }
}
