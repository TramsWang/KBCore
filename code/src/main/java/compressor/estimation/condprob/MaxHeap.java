package compressor.estimation.condprob;

import java.util.*;

public class MaxHeap<T> {
    private static class HeapElement<T> {
        private final Queue<T> queue;
        private final double score;

        public HeapElement(T firstElement, double score) {
            this.queue = new LinkedList<>();
            this.score = score;
            this.queue.offer(firstElement);
        }

        public void push(T element) {
            queue.offer(element);
        }

        public T poll() {
            return queue.poll();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    private final PriorityQueue<HeapElement<T>> heap;
    private final Map<Double, HeapElement<T>> valueMap;

    public MaxHeap() {
        heap = new PriorityQueue<>(Comparator.comparingDouble((HeapElement<T> e) -> e.score).reversed());
        valueMap = new HashMap<>();
    }

    public void add(T element, double score) {
        HeapElement<T> heap_element = valueMap.get(score);
        if (null == heap_element) {
            heap_element = new HeapElement<>(element, score);
            heap.add(heap_element);
            valueMap.put(score, heap_element);
        } else {
            heap_element.push(element);
        }
    }

    public T poll() {
        HeapElement<T> peak = heap.peek();
        if (null == peak) {
            return null;
        }
        T result = peak.poll();
        if (peak.isEmpty()) {
            heap.poll();
            valueMap.remove(peak.score);
        }
        return result;
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }
}
