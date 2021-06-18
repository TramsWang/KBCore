package sinc.impl.cached;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static class CacheStat {
        public final int headCachedEntries;
        public final int bodyCachedEntries;

        public CacheStat(int headCachedEntries, int bodyCachedEntries) {
            this.headCachedEntries = headCachedEntries;
            this.bodyCachedEntries = bodyCachedEntries;
        }
    }
    public final List<CacheStat> cacheStats = new ArrayList<>();

    public void show() {
        System.out.println("### Cached Query Monitored Info ###\n");
        System.out.println("--- Time Cost ---");
        System.out.printf(
                "T(ms) %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n",
                "[Pre", "AllEntail", "+Entail]", "[EBV", "EBV+", "NBV", "NBV+", "Const]", "#clone", "clone");
        System.out.printf(
                "      %10d %10d %10d %10d %10d %10d %10d %10d %10d %10d\n\n",
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

        System.out.println("--- Cache Entry Statistics ---");
        CacheStat max_head = cacheStats.get(0);
        CacheStat max_body = cacheStats.get(0);
        int[] head_entries = new int[cacheStats.size()];
        int[] body_entries = new int[cacheStats.size()];
        for (int i = 0; i < cacheStats.size(); i++) {
            CacheStat cache_stat = cacheStats.get(i);
            head_entries[i] = cache_stat.headCachedEntries;
            body_entries[i] = cache_stat.bodyCachedEntries;
            max_head = (cache_stat.headCachedEntries > max_head.headCachedEntries) ? cache_stat : max_head;
            max_body = (cache_stat.bodyCachedEntries > max_body.bodyCachedEntries) ? cache_stat : max_body;
        }
        System.out.printf(
                "- Max Head Cache Entries: %d (%d in body)\n",
                max_head.headCachedEntries, max_head.bodyCachedEntries
        );
        System.out.printf(
                "- Max Body Cache Entries: %d (%d in head)\n",
                max_body.bodyCachedEntries, max_body.headCachedEntries
        );
        System.out.print("- Head Cache Entries: ");
        System.out.println(Arrays.toString(head_entries));
        System.out.print("- Body Cache Entries: ");
        System.out.println(Arrays.toString(body_entries));
    }
}
