package sinc.util;

import java.util.*;

public class MultiSet<T> {
    private final Map<T, Integer> cntMap;
    private int size = 0;

    public MultiSet() {
        cntMap = new HashMap<>();
    }

    public MultiSet(MultiSet<T> another) {
        this.cntMap = new HashMap<>(another.cntMap);
        this.size = another.size;
    }

    private MultiSet(Map<T, Integer> map) {
        this.cntMap = map;
    }

    public void add(T element) {
        cntMap.compute(element, (k, v) -> (null == v) ? 1 : v + 1);
        size++;
    }

    public void addAll(MultiSet<T> another) {
        for (Map.Entry<T, Integer> entry: another.cntMap.entrySet()) {
            this.cntMap.compute(entry.getKey(), (k, v) -> (null == v) ? entry.getValue() : v + entry.getValue());
        }
    }

    public void remove(T element) {
        cntMap.computeIfPresent(element, (k, v) -> {
            if (1 <= v) {
                size--;
            }
            return (1 < v) ? v - 1 : null;
        });
    }

    public int size() {
        return size;
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
        HashMap<T, Integer> union = new HashMap<>(cntMap);
        for (Map.Entry<T, Integer> entry: another.cntMap.entrySet()) {
            union.compute(
                    entry.getKey(),
                    (k, v) -> (null == v) ? entry.getValue() : Math.max(v, entry.getValue()));
        }
        return new MultiSet<>(union);
    }

    public List<T> elementsAboveProportion(double proportion) {
        List<T> result = new ArrayList<>();
        int threshold = (int)(this.size * proportion);
        for (Map.Entry<T, Integer> entry: cntMap.entrySet()) {
            if (entry.getValue() > threshold) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiSet<?> multiSet = (MultiSet<?>) o;
        return size == multiSet.size && Objects.equals(cntMap, multiSet.cntMap);  // Todo: Is this reliable?
    }

    @Override
    public int hashCode() {
        return Objects.hash(cntMap, size);
    }
}
