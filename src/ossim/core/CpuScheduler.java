package ossim.core;

import ossim.util.InputException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * CPU scheduling: FCFS, SJF, SRTF, Priority (non-preemptive / preemptive) and Round Robin.
 * Lower priority number means higher priority. Ties are broken by arrival time, then input order.
 */
public final class CpuScheduler {

    public enum Algorithm {
        FCFS("FCFS", false),
        SJF("SJF (non-preemptive)", false),
        SRTF("SRTF (preemptive SJF)", false),
        PRIORITY("Priority (non-preemptive)", true),
        PRIORITY_PREEMPTIVE("Priority (preemptive)", true),
        ROUND_ROBIN("Round Robin", false);

        private final String label;
        private final boolean usesPriority;

        Algorithm(String label, boolean usesPriority) {
            this.label = label;
            this.usesPriority = usesPriority;
        }

        public boolean usesPriority() {
            return usesPriority;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record Proc(String id, int arrival, int burst, int priority) { }

    public record Slice(String id, int start, int end) { }

    public record Row(Proc proc, int completion, int turnaround, int waiting, int response) { }

    public record Result(Algorithm algorithm, List<Row> rows, List<Slice> gantt,
                         double avgWaiting, double avgTurnaround, double avgResponse, double cpuUtilization) { }

    public static final String IDLE = "idle";

    private CpuScheduler() { }

    public static Result run(Algorithm algorithm, List<Proc> procs, int quantum) {
        validate(algorithm, procs, quantum);
        Simulation sim = new Simulation(procs);
        switch (algorithm) {
            case FCFS -> sim.runByKey(false, i -> 0);
            case SJF -> sim.runByKey(false, i -> sim.burst[i]);
            case SRTF -> sim.runByKey(true, i -> sim.remaining[i]);
            case PRIORITY -> sim.runByKey(false, i -> sim.priority[i]);
            case PRIORITY_PREEMPTIVE -> sim.runByKey(true, i -> sim.priority[i]);
            case ROUND_ROBIN -> sim.runRoundRobin(quantum);
        }
        return sim.result(algorithm);
    }

    private static void validate(Algorithm algorithm, List<Proc> procs, int quantum) {
        if (procs.isEmpty()) {
            throw new InputException("Add at least one process.");
        }
        if (algorithm == Algorithm.ROUND_ROBIN && quantum < 1) {
            throw new InputException("Time quantum must be at least 1.");
        }
        Set<String> ids = new HashSet<>();
        for (Proc p : procs) {
            if (p.id().isBlank()) {
                throw new InputException("Process ID must not be blank.");
            }
            if (!ids.add(p.id())) {
                throw new InputException("Duplicate process ID: " + p.id());
            }
            if (p.arrival() < 0) {
                throw new InputException(p.id() + ": arrival time cannot be negative.");
            }
            if (p.burst() < 1) {
                throw new InputException(p.id() + ": burst time must be at least 1.");
            }
        }
    }

    /** Mutable state of one scheduling run. */
    private static final class Simulation {
        final List<Proc> procs;
        final int n;
        final int[] arrival;
        final int[] burst;
        final int[] priority;
        final int[] remaining;
        final int[] completion;
        final int[] firstRun;
        final List<Slice> gantt = new ArrayList<>();

        Simulation(List<Proc> procs) {
            this.procs = procs;
            n = procs.size();
            arrival = new int[n];
            burst = new int[n];
            priority = new int[n];
            remaining = new int[n];
            completion = new int[n];
            firstRun = new int[n];
            Arrays.fill(firstRun, -1);
            for (int i = 0; i < n; i++) {
                Proc p = procs.get(i);
                arrival[i] = p.arrival();
                burst[i] = p.burst();
                priority[i] = p.priority();
                remaining[i] = p.burst();
            }
        }

        /** Key-driven scheduling: picks the ready process with the smallest key (preemptive re-evaluates at arrivals). */
        void runByKey(boolean preemptive, IntUnaryOperator key) {
            int time = earliestArrival();
            int finished = 0;
            while (finished < n) {
                int next = pickReady(time, key);
                if (next < 0) {
                    int wake = nextArrivalAfter(time);
                    addSlice(IDLE, time, wake);
                    time = wake;
                    continue;
                }
                int end = time + remaining[next];
                if (preemptive) {
                    end = Math.min(end, nextArrivalAfter(time));
                }
                execute(next, time, end);
                time = end;
                if (remaining[next] == 0) {
                    finished++;
                }
            }
        }

        void runRoundRobin(int quantum) {
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }
            Arrays.sort(order, (a, b) -> Integer.compare(arrival[a], arrival[b]));  // stable: input order on ties

            Deque<Integer> queue = new ArrayDeque<>();
            int time = earliestArrival();
            int cursor = 0;
            int finished = 0;
            while (finished < n) {
                cursor = enqueueArrivals(order, cursor, time, queue);
                if (queue.isEmpty()) {
                    int wake = arrival[order[cursor]];
                    addSlice(IDLE, time, wake);
                    time = wake;
                    continue;
                }
                int current = queue.poll();
                int slice = Math.min(quantum, remaining[current]);
                execute(current, time, time + slice);
                time += slice;
                cursor = enqueueArrivals(order, cursor, time, queue);   // arrivals enter before the preempted process
                if (remaining[current] > 0) {
                    queue.add(current);
                } else {
                    finished++;
                }
            }
        }

        private int enqueueArrivals(Integer[] order, int cursor, int time, Deque<Integer> queue) {
            while (cursor < n && arrival[order[cursor]] <= time) {
                queue.add(order[cursor++]);
            }
            return cursor;
        }

        private int pickReady(int time, IntUnaryOperator key) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (remaining[i] == 0 || arrival[i] > time) {
                    continue;
                }
                if (best < 0) {
                    best = i;
                    continue;
                }
                int ki = key.applyAsInt(i);
                int kb = key.applyAsInt(best);
                if (ki < kb || (ki == kb && arrival[i] < arrival[best])) {
                    best = i;
                }
            }
            return best;
        }

        private int earliestArrival() {
            int min = Integer.MAX_VALUE;
            for (int a : arrival) {
                min = Math.min(min, a);
            }
            return min;
        }

        private int nextArrivalAfter(int time) {
            int next = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (remaining[i] > 0 && arrival[i] > time) {
                    next = Math.min(next, arrival[i]);
                }
            }
            return next;
        }

        private void execute(int i, int from, int to) {
            if (firstRun[i] < 0) {
                firstRun[i] = from;
            }
            remaining[i] -= to - from;
            if (remaining[i] == 0) {
                completion[i] = to;
            }
            addSlice(procs.get(i).id(), from, to);
        }

        private void addSlice(String id, int from, int to) {
            int last = gantt.size() - 1;
            if (last >= 0 && gantt.get(last).id().equals(id) && gantt.get(last).end() == from) {
                gantt.set(last, new Slice(id, gantt.get(last).start(), to));
            } else {
                gantt.add(new Slice(id, from, to));
            }
        }

        Result result(Algorithm algorithm) {
            List<Row> rows = new ArrayList<>(n);
            double sumWait = 0;
            double sumTurn = 0;
            double sumResp = 0;
            long busy = 0;
            int finish = 0;
            for (int i = 0; i < n; i++) {
                int turnaround = completion[i] - arrival[i];
                int waiting = turnaround - burst[i];
                int response = firstRun[i] - arrival[i];
                rows.add(new Row(procs.get(i), completion[i], turnaround, waiting, response));
                sumWait += waiting;
                sumTurn += turnaround;
                sumResp += response;
                busy += burst[i];
                finish = Math.max(finish, completion[i]);
            }
            double span = finish - earliestArrival();
            return new Result(algorithm, rows, gantt, sumWait / n, sumTurn / n, sumResp / n, busy * 100.0 / span);
        }
    }
}
