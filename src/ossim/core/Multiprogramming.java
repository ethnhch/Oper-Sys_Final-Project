package ossim.core;

import ossim.util.InputException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multiprogramming: jobs are admitted FCFS into main memory while space allows; resident jobs share the CPU
 * by Round Robin. Reports memory use, CPU utilization and degree of multiprogramming.
 */
public final class Multiprogramming {

    public record Job(String id, int arrival, int memory, int burst) { }

    /** System status during [start, end). running is "idle" when the CPU has nothing to run. */
    public record Snapshot(int start, int end, String running, List<String> resident, int memoryUsed, List<String> waiting) { }

    public record JobStat(Job job, int admitted, int completion, int turnaround, int waiting) { }

    public record Result(List<Snapshot> timeline, List<JobStat> jobs, int totalMemory, int finish,
                         double cpuUtilization, double avgMemoryUtilization, int maxDegree, double throughput) { }

    public static final String IDLE = "idle";

    private Multiprogramming() { }

    public static Result run(List<Job> jobs, int totalMemory, int quantum) {
        validate(jobs, totalMemory, quantum);
        return new Simulation(jobs, totalMemory, quantum).execute();
    }

    private static void validate(List<Job> jobs, int totalMemory, int quantum) {
        if (jobs.isEmpty()) {
            throw new InputException("Add at least one program.");
        }
        if (totalMemory < 1 || quantum < 1) {
            throw new InputException("Total memory and time quantum must be at least 1.");
        }
        Set<String> ids = new HashSet<>();
        for (Job j : jobs) {
            if (!ids.add(j.id())) {
                throw new InputException("Duplicate program ID: " + j.id());
            }
            if (j.memory() > totalMemory) {
                throw new InputException(j.id() + " needs " + j.memory() + " memory, which exceeds total memory " + totalMemory + ".");
            }
        }
    }

    private static final class Simulation {
        final List<Job> jobs;
        final int totalMemory;
        final int quantum;
        final int n;
        final int[] remaining;
        final int[] admitted;
        final int[] completion;
        final Deque<Integer> pending = new ArrayDeque<>();
        final Deque<Integer> ready = new ArrayDeque<>();
        final List<Snapshot> timeline = new ArrayList<>();
        int memoryUsed;
        int maxDegree;
        int lastRelease;   // last instant at which memory was freed

        Simulation(List<Job> jobs, int totalMemory, int quantum) {
            this.jobs = jobs;
            this.totalMemory = totalMemory;
            this.quantum = quantum;
            n = jobs.size();
            remaining = new int[n];
            admitted = new int[n];
            completion = new int[n];
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
                remaining[i] = jobs.get(i).burst();
            }
            Arrays.sort(order, (a, b) -> Integer.compare(jobs.get(a).arrival(), jobs.get(b).arrival()));
            pending.addAll(Arrays.asList(order));
        }

        Result execute() {
            int start = jobs.get(pending.peek()).arrival();
            int time = start;
            int finished = 0;
            long busy = 0;
            while (finished < n) {
                admit(time);
                if (ready.isEmpty()) {
                    int wake = jobs.get(pending.peek()).arrival();
                    timeline.add(new Snapshot(time, wake, IDLE, List.of(), 0, List.of()));
                    time = wake;
                    continue;
                }
                int current = ready.poll();
                int slice = Math.min(quantum, remaining[current]);
                timeline.add(snapshot(time, time + slice, current));
                time += slice;
                busy += slice;
                remaining[current] -= slice;
                if (remaining[current] == 0) {
                    memoryUsed -= jobs.get(current).memory();
                    completion[current] = time;
                    lastRelease = time;
                    finished++;
                }
                admit(time);
                if (remaining[current] > 0) {
                    ready.add(current);
                }
            }
            return buildResult(start, time, busy);
        }

        /**
         * FCFS admission: the head of the job queue blocks later jobs if it does not fit.
         * Dispatch decisions are made at quantum boundaries; the recorded admission instant is exact.
         */
        private void admit(int time) {
            while (!pending.isEmpty()) {
                int next = pending.peek();
                Job job = jobs.get(next);
                if (job.arrival() > time || memoryUsed + job.memory() > totalMemory) {
                    return;
                }
                pending.poll();
                admitted[next] = Math.max(job.arrival(), lastRelease);   // true instant it could enter memory
                memoryUsed += job.memory();
                ready.add(next);
            }
        }

        private Snapshot snapshot(int from, int to, int running) {
            List<String> resident = new ArrayList<>();
            resident.add(jobs.get(running).id());
            ready.forEach(i -> resident.add(jobs.get(i).id()));
            maxDegree = Math.max(maxDegree, resident.size());
            List<String> waiting = new ArrayList<>();
            for (int i : pending) {
                if (jobs.get(i).arrival() > from) {
                    break;
                }
                waiting.add(jobs.get(i).id());
            }
            return new Snapshot(from, to, jobs.get(running).id(), resident, memoryUsed, waiting);
        }

        private Result buildResult(int start, int finish, long busy) {
            List<JobStat> stats = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Job job = jobs.get(i);
                int turnaround = completion[i] - job.arrival();
                stats.add(new JobStat(job, admitted[i], completion[i], turnaround, turnaround - job.burst()));
            }
            double span = finish - start;
            double memoryTime = 0;
            for (Snapshot s : timeline) {
                memoryTime += (double) s.memoryUsed() * (s.end() - s.start());
            }
            return new Result(timeline, stats, totalMemory, finish, busy * 100.0 / span,
                    memoryTime * 100.0 / (span * totalMemory), maxDegree, n / span);
        }
    }
}
