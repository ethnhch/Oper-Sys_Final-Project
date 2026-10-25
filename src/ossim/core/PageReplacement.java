package ossim.core;

import ossim.util.InputException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Page replacement: FIFO, LRU, Optimal and Second-Chance (Clock). */
public final class PageReplacement {

    public enum Algorithm {
        FIFO("FIFO"), LRU("LRU"), OPTIMAL("Optimal"), CLOCK("Second Chance (Clock)");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** One reference: frames holds the content of every frame after the access (null = empty). */
    public record Step(int index, int page, List<Integer> frames, boolean hit, Integer evicted) { }

    public record Result(Algorithm algorithm, List<Step> steps, int hits, int faults) {
        public double hitRatio() {
            return steps.isEmpty() ? 0 : hits * 100.0 / steps.size();
        }

        public double faultRatio() {
            return 100.0 - hitRatio();
        }
    }

    private PageReplacement() { }

    public static Result run(Algorithm algorithm, List<Integer> references, int frameCount) {
        if (references.isEmpty()) {
            throw new InputException("The reference string is empty.");
        }
        if (frameCount < 1) {
            throw new InputException("Number of frames must be at least 1.");
        }
        int[] nextUse = nextOccurrence(references);
        Integer[] frames = new Integer[frameCount];
        int[] lastUsed = new int[frameCount];
        int[] upcoming = new int[frameCount];
        boolean[] referenced = new boolean[frameCount];
        int hand = 0;
        int hits = 0;
        List<Step> steps = new ArrayList<>(references.size());

        for (int i = 0; i < references.size(); i++) {
            int page = references.get(i);
            int slot = Arrays.asList(frames).indexOf(page);
            boolean hit = slot >= 0;
            Integer evicted = null;
            if (hit) {
                hits++;
            } else {
                slot = Arrays.asList(frames).indexOf(null);
                if (slot < 0) {
                    slot = switch (algorithm) {
                        case FIFO -> hand;
                        case LRU -> indexOfMin(lastUsed);
                        case OPTIMAL -> indexOfMax(upcoming);
                        case CLOCK -> secondChanceVictim(referenced, hand);
                    };
                    evicted = frames[slot];
                }
                frames[slot] = page;
                hand = (slot + 1) % frameCount;
            }
            lastUsed[slot] = i;
            upcoming[slot] = nextUse[i];
            referenced[slot] = true;
            steps.add(new Step(i, page, Arrays.asList(frames.clone()), hit, evicted));
        }
        return new Result(algorithm, steps, hits, references.size() - hits);
    }

    /** nextUse[i] = index of the next reference to the same page after i, or Integer.MAX_VALUE if never again. */
    private static int[] nextOccurrence(List<Integer> refs) {
        int[] next = new int[refs.size()];
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = refs.size() - 1; i >= 0; i--) {
            next[i] = seen.getOrDefault(refs.get(i), Integer.MAX_VALUE);
            seen.put(refs.get(i), i);
        }
        return next;
    }

    /** Clears reference bits while sweeping; returns the first frame whose bit is already 0. */
    private static int secondChanceVictim(boolean[] referenced, int hand) {
        int slot = hand;
        while (referenced[slot]) {
            referenced[slot] = false;
            slot = (slot + 1) % referenced.length;
        }
        return slot;
    }

    private static int indexOfMin(int[] values) {
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[best]) {
                best = i;
            }
        }
        return best;
    }

    private static int indexOfMax(int[] values) {
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[best]) {
                best = i;
            }
        }
        return best;
    }
}
