package ossim.core;

import ossim.util.InputException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Disk scheduling: FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK.
 * SCAN/C-SCAN travel to the disk edge; LOOK/C-LOOK turn at the last request.
 * The C-SCAN / C-LOOK return jump is counted in total movement only when requested.
 */
public final class DiskScheduler {

    public enum Algorithm {
        FCFS("FCFS"), SSTF("SSTF"), SCAN("SCAN"), C_SCAN("C-SCAN"), LOOK("LOOK"), C_LOOK("C-LOOK");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        public boolean usesDirection() {
            return this != FCFS && this != SSTF;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Direction { UP, DOWN }

    public enum Kind { SERVE, EDGE, JUMP }

    public record Move(int from, int to, Kind kind) {
        public int distance() {
            return Math.abs(to - from);
        }
    }

    public record Result(Algorithm algorithm, List<Integer> serviceOrder, List<Move> moves, int totalMovement, boolean jumpCounted) { }

    private DiskScheduler() { }

    public static Result run(Algorithm algorithm, List<Integer> requests, int head, int maxCylinder,
                             Direction direction, boolean countJump) {
        validate(requests, head, maxCylinder);
        Trace trace = new Trace(head);
        switch (algorithm) {
            case FCFS -> requests.forEach(trace::serve);
            case SSTF -> sstf(requests, trace);
            default -> sweep(algorithm, requests, head, maxCylinder, direction, trace);
        }
        int total = 0;
        for (Move m : trace.moves) {
            if (m.kind() != Kind.JUMP || countJump) {
                total += m.distance();
            }
        }
        return new Result(algorithm, trace.order, trace.moves, total, countJump);
    }

    private static void sstf(List<Integer> requests, Trace trace) {
        List<Integer> pending = new ArrayList<>(requests);
        while (!pending.isEmpty()) {
            int best = 0;
            for (int i = 1; i < pending.size(); i++) {
                if (Math.abs(pending.get(i) - trace.position) < Math.abs(pending.get(best) - trace.position)) {
                    best = i;
                }
            }
            trace.serve(pending.remove(best));
        }
    }

    private static void sweep(Algorithm algorithm, List<Integer> requests, int head, int max,
                              Direction direction, Trace trace) {
        boolean up = direction == Direction.UP;
        List<Integer> ahead = new ArrayList<>();
        List<Integer> behind = new ArrayList<>();
        for (int r : requests) {
            boolean inFront = up ? r >= head : r <= head;
            (inFront ? ahead : behind).add(r);
        }
        Collections.sort(ahead);
        Collections.sort(behind);
        if (!up) {
            Collections.reverse(ahead);
        }
        ahead.forEach(trace::serve);

        int edge = up ? max : 0;
        int oppositeEdge = up ? 0 : max;
        // Reverse sweep visits requests nearest the edge first; circular sweep restarts from the opposite end.
        List<Integer> reverseOrder = new ArrayList<>(behind);
        if (up) {
            Collections.reverse(reverseOrder);
        }
        List<Integer> circularOrder = new ArrayList<>(behind);
        if (!up) {
            Collections.reverse(circularOrder);
        }

        switch (algorithm) {
            case SCAN -> {
                trace.edge(edge);
                reverseOrder.forEach(trace::serve);
            }
            case LOOK -> reverseOrder.forEach(trace::serve);
            case C_SCAN -> {
                trace.edge(edge);
                if (!behind.isEmpty()) {
                    trace.jump(oppositeEdge);
                    circularOrder.forEach(trace::serve);
                }
            }
            case C_LOOK -> {
                if (!behind.isEmpty()) {
                    trace.jump(circularOrder.get(0));
                    circularOrder.forEach(trace::serve);
                }
            }
            default -> throw new IllegalStateException("Not a sweep algorithm: " + algorithm);
        }
    }

    private static void validate(List<Integer> requests, int head, int max) {
        if (requests.isEmpty()) {
            throw new InputException("Enter at least one disk request.");
        }
        if (max < 1) {
            throw new InputException("Highest cylinder must be at least 1.");
        }
        if (head < 0 || head > max) {
            throw new InputException("Initial head position " + head + " must be between 0 and " + max + ".");
        }
        for (int r : requests) {
            if (r < 0 || r > max) {
                throw new InputException("Request " + r + " is outside the disk range 0.." + max + ".");
            }
        }
    }

    /** Records head travel. */
    private static final class Trace {
        int position;
        final List<Move> moves = new ArrayList<>();
        final List<Integer> order = new ArrayList<>();

        Trace(int start) {
            position = start;
        }

        void serve(int cylinder) {
            moves.add(new Move(position, cylinder, Kind.SERVE));
            order.add(cylinder);
            position = cylinder;
        }

        void edge(int cylinder) {
            if (cylinder != position) {
                moves.add(new Move(position, cylinder, Kind.EDGE));
                position = cylinder;
            }
        }

        void jump(int cylinder) {
            if (cylinder != position) {
                moves.add(new Move(position, cylinder, Kind.JUMP));
                position = cylinder;
            }
        }
    }
}
