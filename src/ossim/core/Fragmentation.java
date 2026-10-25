package ossim.core;

import ossim.util.InputException;

import java.util.ArrayList;
import java.util.List;

/**
 * Contiguous allocation with First/Best/Worst/Next Fit.
 * FIXED: each block holds one process, so the unused rest of the block is internal fragmentation.
 * DYNAMIC: the block is split, so leftover holes may cause external fragmentation.
 */
public final class Fragmentation {

    public enum Strategy {
        FIRST_FIT("First Fit"), BEST_FIT("Best Fit"), WORST_FIT("Worst Fit"), NEXT_FIT("Next Fit");

        private final String label;

        Strategy(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Mode {
        FIXED("Fixed partitions"), DYNAMIC("Dynamic (split) partitions");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record Request(String id, int size) { }

    /** block is -1 when the process could not be placed. */
    public record Allocation(Request request, int block, int internalWaste, String note) { }

    public record BlockState(int index, int size, List<String> residents, int used, int free) { }

    public record Result(Strategy strategy, Mode mode, List<Allocation> allocations, List<BlockState> blocks,
                         int internalFragmentation, int totalFree, int largestHole, int externalFailures) { }

    private Fragmentation() { }

    public static Result run(Strategy strategy, Mode mode, List<Integer> blockSizes, List<Request> requests) {
        if (blockSizes.isEmpty() || requests.isEmpty()) {
            throw new InputException("Enter at least one memory block and one process.");
        }
        int[] size = blockSizes.stream().mapToInt(Integer::intValue).toArray();
        int[] free = size.clone();
        int[] used = new int[size.length];
        List<List<String>> residents = new ArrayList<>();
        for (int i = 0; i < size.length; i++) {
            residents.add(new ArrayList<>());
        }

        List<Allocation> allocations = new ArrayList<>();
        int internal = 0;
        int externalFailures = 0;
        int last = 0;
        for (Request req : requests) {
            int block = choose(strategy, free, req.size(), last);
            if (block < 0) {
                int totalFree = sum(free);
                boolean external = totalFree >= req.size();
                externalFailures += external ? 1 : 0;
                String why = external
                        ? "EXTERNAL fragmentation: " + totalFree + " free in total, but no single hole >= " + req.size()
                        : "Not enough free memory (" + totalFree + " free)";
                allocations.add(new Allocation(req, -1, 0, why));
                continue;
            }
            last = block;
            residents.get(block).add(req.id());
            used[block] += req.size();
            int waste = 0;
            if (mode == Mode.FIXED) {
                waste = free[block] - req.size();
                free[block] = 0;
                internal += waste;
            } else {
                free[block] -= req.size();
            }
            allocations.add(new Allocation(req, block, waste, waste > 0 ? "Internal fragmentation: " + waste : "Allocated"));
        }

        List<BlockState> states = new ArrayList<>();
        int largest = 0;
        for (int i = 0; i < size.length; i++) {
            largest = Math.max(largest, free[i]);
            int leftover = mode == Mode.FIXED ? (residents.get(i).isEmpty() ? size[i] : size[i] - used[i]) : free[i];
            states.add(new BlockState(i, size[i], residents.get(i), used[i], leftover));
        }
        return new Result(strategy, mode, allocations, states, internal, sum(free), largest, externalFailures);
    }

    private static int choose(Strategy strategy, int[] free, int need, int last) {
        int n = free.length;
        int chosen = -1;
        switch (strategy) {
            case FIRST_FIT -> {
                for (int i = 0; i < n; i++) {
                    if (free[i] >= need) {
                        return i;
                    }
                }
            }
            case NEXT_FIT -> {
                for (int k = 0; k < n; k++) {
                    int i = (last + k) % n;
                    if (free[i] >= need) {
                        return i;
                    }
                }
            }
            case BEST_FIT -> {
                for (int i = 0; i < n; i++) {
                    if (free[i] >= need && (chosen < 0 || free[i] < free[chosen])) {
                        chosen = i;
                    }
                }
            }
            case WORST_FIT -> {
                for (int i = 0; i < n; i++) {
                    if (free[i] >= need && (chosen < 0 || free[i] > free[chosen])) {
                        chosen = i;
                    }
                }
            }
        }
        return chosen;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int v : values) {
            total += v;
        }
        return total;
    }
}
