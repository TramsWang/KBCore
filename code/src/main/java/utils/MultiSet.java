package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MultiSet<T> {
    private final Map<T, Integer> cntMap;

    public MultiSet() {
        cntMap = new HashMap<>();
    }

    private MultiSet(Map<T, Integer> map) {
        this.cntMap = map;
    }

    public void add(T element) {
        cntMap.compute(element, (k, v) -> (null == v) ? 1 : v + 1);
    }

    public int size() {
        int cnt = 0;
        for (Integer element_cnt: cntMap.values()) {
            cnt += element_cnt;
        }
        return cnt;
    }

    public double jaccardSimilarity(MultiSet<T> another) {
        MultiSet<T> intersection = this.intersection(another);
        MultiSet<T> union = this.union(another);
        double intersection_size = intersection.size();
        double union_sieze = union.size();
        return intersection_size / union_sieze;
    }

    public MultiSet<T> intersection(MultiSet<T> another) {
        Map<T, Integer> intersection = new HashMap<>();
        Set<Map.Entry<T, Integer>> entry_set;
        Map<T, Integer> compared_map;
        if (this.cntMap.keySet().size() <= another.cntMap.keySet().size()) {
            entry_set = this.cntMap.entrySet();
            compared_map = another.cntMap;
        } else {
            entry_set = another.cntMap.entrySet();
            compared_map = this.cntMap;
        }
        for (Map.Entry<T, Integer> entry: entry_set) {
            Integer compared_cnt = compared_map.get(entry.getKey());
            if (null != compared_cnt) {
                intersection.put(entry.getKey(), Math.min(entry.getValue(), compared_cnt));
            }
        }
        return new MultiSet<>(intersection);
    }

    public MultiSet<T> union(MultiSet<T> another) {
        HashMap<T, Integer> intersection = new HashMap<>(cntMap);
        for (Map.Entry<T, Integer> entry: another.cntMap.entrySet()) {
            intersection.compute(
                    entry.getKey(),
                    (k, v) -> (null == v) ? entry.getValue() : Math.max(v, entry.getValue()));
        }
        return new MultiSet<>(intersection);
    }
}
