package ossim;

import ossim.core.*;
import ossim.core.CpuScheduler.Algorithm;
import ossim.core.CpuScheduler.Proc;
import ossim.util.InputException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Dependency-free test runner: textbook results (manual verification), boundaries, invalid input and large datasets. */
public final class SelfTest {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        cpu();
        banker();
        paging();
        pageReplacement();
        segmentation();
        fragmentation();
        disk();
        multiprogramming();
        largeDatasets();
        System.out.printf("%nPassed: %d   Failed: %d%n", passed, failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---------------------------------------------------------------- CPU
    private static void cpu() {
        List<Proc> burst = List.of(new Proc("P1", 0, 24, 0), new Proc("P2", 0, 3, 0), new Proc("P3", 0, 3, 0));
        check("CPU FCFS avg waiting = 17", 17.0, CpuScheduler.run(Algorithm.FCFS, burst, 1).avgWaiting());
        check("CPU SJF avg waiting = 3", 3.0, CpuScheduler.run(Algorithm.SJF, burst, 1).avgWaiting());
        check("CPU RR q=4 avg waiting = 17/3", 17.0 / 3, CpuScheduler.run(Algorithm.ROUND_ROBIN, burst, 4).avgWaiting());

        List<Proc> mixed = List.of(new Proc("P1", 0, 8, 3), new Proc("P2", 1, 4, 1), new Proc("P3", 2, 9, 4), new Proc("P4", 3, 5, 2));
        check("CPU SRTF avg waiting = 6.5", 6.5, CpuScheduler.run(Algorithm.SRTF, mixed, 1).avgWaiting());
        check("CPU Priority avg waiting = 7.75", 7.75, CpuScheduler.run(Algorithm.PRIORITY, mixed, 1).avgWaiting());
        check("CPU Priority-P completes P2 at 5", 5, CpuScheduler.run(Algorithm.PRIORITY_PREEMPTIVE, mixed, 1).rows().get(1).completion());

        List<Proc> gap = List.of(new Proc("A", 0, 2, 0), new Proc("B", 10, 2, 0));
        CpuScheduler.Result idle = CpuScheduler.run(Algorithm.FCFS, gap, 1);
        check("CPU idle gap appears in Gantt", 3, idle.gantt().size());
        check("CPU single process", 5, CpuScheduler.run(Algorithm.ROUND_ROBIN, List.of(new Proc("X", 0, 5, 0)), 2).rows().get(0).completion());
        expectInvalid("CPU burst 0 rejected", () -> CpuScheduler.run(Algorithm.FCFS, List.of(new Proc("A", 0, 0, 0)), 1));
        expectInvalid("CPU duplicate id rejected", () -> CpuScheduler.run(Algorithm.FCFS, List.of(new Proc("A", 0, 1, 0), new Proc("A", 1, 1, 0)), 1));
        expectInvalid("CPU quantum 0 rejected", () -> CpuScheduler.run(Algorithm.ROUND_ROBIN, burst, 0));
        expectInvalid("CPU empty rejected", () -> CpuScheduler.run(Algorithm.FCFS, List.of(), 1));
    }

    // ---------------------------------------------------------------- Banker
    private static final int[][] ALLOC = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}};
    private static final int[][] MAX = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}};

    private static void banker() {
        BankersAlgorithm.Result r = BankersAlgorithm.analyze(ALLOC, MAX, new int[]{3, 3, 2});
        check("Banker safe", true, r.safe());
        check("Banker sequence", List.of(1, 3, 4, 0, 2), r.safeSequence());
        check("Banker need P0", Arrays.toString(new int[]{7, 4, 3}), Arrays.toString(r.need()[0]));
        BankersAlgorithm.RequestResult grant = BankersAlgorithm.request(ALLOC, MAX, new int[]{3, 3, 2}, 1, new int[]{1, 0, 2});
        check("Banker P1 request (1,0,2) granted", true, grant.granted());
        int[][] alloc2 = ALLOC.clone();
        alloc2[1] = new int[]{3, 0, 2};
        BankersAlgorithm.RequestResult wait = BankersAlgorithm.request(alloc2, MAX, new int[]{2, 3, 0}, 4, new int[]{3, 3, 0});
        check("Banker P4 request (3,3,0) must wait", false, wait.granted());
        BankersAlgorithm.RequestResult unsafe = BankersAlgorithm.request(alloc2, MAX, new int[]{2, 3, 0}, 0, new int[]{0, 2, 0});
        check("Banker P0 request (0,2,0) unsafe", false, unsafe.granted());
        check("Banker unsafe state", false, BankersAlgorithm.analyze(new int[][]{{1}, {1}}, new int[][]{{2}, {2}}, new int[]{0}).safe());
        expectInvalid("Banker Allocation > Max rejected", () -> BankersAlgorithm.analyze(new int[][]{{3}}, new int[][]{{2}}, new int[]{1}));
        expectInvalid("Banker negative available rejected", () -> BankersAlgorithm.analyze(new int[][]{{0}}, new int[][]{{2}}, new int[]{-1}));
    }

    // ---------------------------------------------------------------- Paging
    private static void paging() {
        List<Paging.Entry> table = List.of(new Paging.Entry(0, 5), new Paging.Entry(1, 2));
        List<Paging.Translation> t = Paging.translate(1024, 8, table, List.of(100L, 1500L, 5000L, 0L, 1023L));
        check("Paging 100 -> 5220", 5220L, t.get(0).physical());
        check("Paging 1500 -> 2*1024+476", 2524L, t.get(1).physical());
        check("Paging unmapped page faults", null, t.get(2).physical());
        check("Paging boundary 1023 -> 5*1024+1023", 6143L, t.get(4).physical());
        expectInvalid("Paging frame out of range rejected", () -> Paging.translate(1024, 2, table, List.of(1L)));
        expectInvalid("Paging duplicate frame rejected", () -> Paging.translate(1024, 8, List.of(new Paging.Entry(0, 1), new Paging.Entry(1, 1)), List.of(1L)));
    }

    // ---------------------------------------------------------------- Page replacement
    private static void pageReplacement() {
        List<Integer> refs = List.of(7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1);
        check("PR FIFO faults = 15", 15, PageReplacement.run(PageReplacement.Algorithm.FIFO, refs, 3).faults());
        check("PR LRU faults = 12", 12, PageReplacement.run(PageReplacement.Algorithm.LRU, refs, 3).faults());
        check("PR OPT faults = 9", 9, PageReplacement.run(PageReplacement.Algorithm.OPTIMAL, refs, 3).faults());
        List<Integer> belady = List.of(1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5);
        check("PR Belady FIFO 3 frames = 9", 9, PageReplacement.run(PageReplacement.Algorithm.FIFO, belady, 3).faults());
        check("PR Belady FIFO 4 frames = 10", 10, PageReplacement.run(PageReplacement.Algorithm.FIFO, belady, 4).faults());
        check("PR 1 frame, alternating = all faults", 6, PageReplacement.run(PageReplacement.Algorithm.LRU, List.of(1, 2, 1, 2, 1, 2), 1).faults());
        check("PR frames > distinct pages = distinct faults", 3, PageReplacement.run(PageReplacement.Algorithm.FIFO, List.of(1, 2, 3, 1, 2, 3), 10).faults());
        check("PR Clock faults (textbook string) = 14", 14, PageReplacement.run(PageReplacement.Algorithm.CLOCK, refs, 3).faults());
        expectInvalid("PR zero frames rejected", () -> PageReplacement.run(PageReplacement.Algorithm.FIFO, refs, 0));
    }

    // ---------------------------------------------------------------- Segmentation
    private static void segmentation() {
        List<Segmentation.Segment> table = List.of(new Segmentation.Segment(0, 1400, 1000), new Segmentation.Segment(2, 4300, 400),
                new Segmentation.Segment(3, 3200, 1100));
        List<Segmentation.Translation> t = Segmentation.translate(table, List.of(new Segmentation.Reference(2, 53),
                new Segmentation.Reference(3, 852), new Segmentation.Reference(0, 1222), new Segmentation.Reference(9, 0),
                new Segmentation.Reference(0, 999), new Segmentation.Reference(0, 1000)));
        check("Seg (2,53) -> 4353", 4353L, t.get(0).physical());
        check("Seg (3,852) -> 4052", 4052L, t.get(1).physical());
        check("Seg offset beyond limit is error", false, t.get(2).valid());
        check("Seg undefined segment is error", false, t.get(3).valid());
        check("Seg offset = limit-1 valid", true, t.get(4).valid());
        check("Seg offset = limit invalid", false, t.get(5).valid());
        check("Seg overlap detected", 1, Segmentation.overlapWarnings(List.of(new Segmentation.Segment(0, 0, 100), new Segmentation.Segment(1, 50, 100))).size());
        expectInvalid("Seg duplicate id rejected", () -> Segmentation.translate(List.of(new Segmentation.Segment(0, 0, 1), new Segmentation.Segment(0, 5, 1)), List.of()));
    }

    // ---------------------------------------------------------------- Fragmentation
    private static void fragmentation() {
        List<Integer> blocks = List.of(100, 500, 200, 300, 600);
        List<Fragmentation.Request> procs = List.of(new Fragmentation.Request("P1", 212), new Fragmentation.Request("P2", 417),
                new Fragmentation.Request("P3", 112), new Fragmentation.Request("P4", 426));
        Fragmentation.Result best = Fragmentation.run(Fragmentation.Strategy.BEST_FIT, Fragmentation.Mode.FIXED, blocks, procs);
        check("Frag best fit places every process", 0L, best.allocations().stream().filter(a -> a.block() < 0).count());
        check("Frag best fit internal = 88+83+88+174", 433, best.internalFragmentation());
        Fragmentation.Result first = Fragmentation.run(Fragmentation.Strategy.FIRST_FIT, Fragmentation.Mode.FIXED, blocks, procs);
        check("Frag first fit leaves P4 unplaced", -1, first.allocations().get(3).block());
        Fragmentation.Result dyn = Fragmentation.run(Fragmentation.Strategy.FIRST_FIT, Fragmentation.Mode.DYNAMIC, blocks, procs);
        check("Frag dynamic detects external fragmentation", 1, dyn.externalFailures());
        check("Frag dynamic internal = 0", 0, dyn.internalFragmentation());
        Fragmentation.Result worst = Fragmentation.run(Fragmentation.Strategy.WORST_FIT, Fragmentation.Mode.FIXED, blocks, procs);
        check("Frag worst fit P1 -> block 4 (600)", 4, worst.allocations().get(0).block());
        Fragmentation.Result next = Fragmentation.run(Fragmentation.Strategy.NEXT_FIT, Fragmentation.Mode.FIXED, List.of(10, 20, 30), List.of(new Fragmentation.Request("A", 5), new Fragmentation.Request("B", 5)));
        check("Frag next fit continues after last block", 1, next.allocations().get(1).block());
        expectInvalid("Frag empty blocks rejected", () -> Fragmentation.run(Fragmentation.Strategy.BEST_FIT, Fragmentation.Mode.FIXED, List.of(), procs));
    }

    // ---------------------------------------------------------------- Disk
    private static void disk() {
        List<Integer> req = List.of(98, 183, 37, 122, 14, 124, 65, 67);
        int[] expected = {640, 236, 331, 382, 299, 322};
        DiskScheduler.Algorithm[] algos = DiskScheduler.Algorithm.values();
        for (int i = 0; i < algos.length; i++) {
            DiskScheduler.Result r = DiskScheduler.run(algos[i], req, 53, 199, DiskScheduler.Direction.UP, true);
            check("Disk " + algos[i] + " head movement = " + expected[i], expected[i], r.totalMovement());
            check("Disk " + algos[i] + " serves every request", req.size(), r.serviceOrder().size());
        }
        check("Disk C-SCAN without return jump = 183", 183, DiskScheduler.run(DiskScheduler.Algorithm.C_SCAN, req, 53, 199, DiskScheduler.Direction.UP, false).totalMovement());
        check("Disk SCAN down (53->0->183) = 236", 236, DiskScheduler.run(DiskScheduler.Algorithm.SCAN, req, 53, 199, DiskScheduler.Direction.DOWN, true).totalMovement());
        check("Disk head on request", 0, DiskScheduler.run(DiskScheduler.Algorithm.FCFS, List.of(5), 5, 10, DiskScheduler.Direction.UP, true).totalMovement());
        expectInvalid("Disk request beyond limit rejected", () -> DiskScheduler.run(DiskScheduler.Algorithm.FCFS, List.of(500), 5, 199, DiskScheduler.Direction.UP, true));
        expectInvalid("Disk head beyond limit rejected", () -> DiskScheduler.run(DiskScheduler.Algorithm.FCFS, List.of(5), 500, 199, DiskScheduler.Direction.UP, true));
    }

    // ---------------------------------------------------------------- Multiprogramming
    private static void multiprogramming() {
        List<Multiprogramming.Job> jobs = List.of(new Multiprogramming.Job("J1", 0, 40, 10), new Multiprogramming.Job("J2", 0, 30, 6),
                new Multiprogramming.Job("J3", 0, 50, 4));
        Multiprogramming.Result r = Multiprogramming.run(jobs, 100, 4);
        check("Multi: J3 cannot be admitted at t=0 (70+50>100)", true, r.jobs().get(2).admitted() > 0);
        check("Multi: max degree = 2", 2, r.maxDegree());
        check("Multi: CPU utilization 100% (no gaps)", 100.0, r.cpuUtilization());
        check("Multi: finish = total burst", 20, r.finish());
        expectInvalid("Multi: job larger than memory rejected", () -> Multiprogramming.run(jobs, 45, 4));
    }

    // ---------------------------------------------------------------- Large datasets (stability + invariants)
    private static void largeDatasets() {
        Random rnd = new Random(42);
        int n = 5000;
        List<Proc> procs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            procs.add(new Proc("P" + i, rnd.nextInt(10000), 1 + rnd.nextInt(30), rnd.nextInt(10)));
        }
        for (Algorithm a : Algorithm.values()) {
            CpuScheduler.Result r = CpuScheduler.run(a, procs, 4);
            long totalRun = r.gantt().stream().filter(s -> !s.id().equals(CpuScheduler.IDLE)).mapToLong(s -> s.end() - s.start()).sum();
            long totalBurst = procs.stream().mapToLong(Proc::burst).sum();
            check("Large CPU " + a + " (5000 procs): executed time = total burst", totalBurst, totalRun);
        }
        check("Large CPU: SJF waits <= FCFS waits",
                true, CpuScheduler.run(Algorithm.SJF, procs, 1).avgWaiting() <= CpuScheduler.run(Algorithm.FCFS, procs, 1).avgWaiting());

        int p = 300, m = 8;
        int[][] alloc = new int[p][m], max = new int[p][m];
        int[] avail = new int[m];
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < m; j++) {
                max[i][j] = rnd.nextInt(10);
                alloc[i][j] = rnd.nextInt(max[i][j] + 1) / 2;
            }
        }
        Arrays.fill(avail, 400);
        BankersAlgorithm.Result br = BankersAlgorithm.analyze(alloc, max, avail);
        check("Large Banker (300x8) is safe with ample resources", true, br.safe());
        check("Large Banker sequence has every process once", p, br.safeSequence().stream().distinct().count());

        List<Integer> refs = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            refs.add(rnd.nextInt(60));
        }
        int opt = PageReplacement.run(PageReplacement.Algorithm.OPTIMAL, refs, 10).faults();
        check("Large PR (20000 refs): OPT <= LRU", true, opt <= PageReplacement.run(PageReplacement.Algorithm.LRU, refs, 10).faults());
        check("Large PR (20000 refs): OPT <= FIFO", true, opt <= PageReplacement.run(PageReplacement.Algorithm.FIFO, refs, 10).faults());

        List<Integer> disk = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            disk.add(rnd.nextInt(5000));
        }
        for (DiskScheduler.Algorithm a : DiskScheduler.Algorithm.values()) {
            check("Large Disk " + a + " (20000 requests) serves all", 20000, DiskScheduler.run(a, disk, 2500, 4999, DiskScheduler.Direction.UP, true).serviceOrder().size());
        }
        check("Large Disk: SSTF <= FCFS", true,
                DiskScheduler.run(DiskScheduler.Algorithm.SSTF, disk, 2500, 4999, DiskScheduler.Direction.UP, true).totalMovement()
                        <= DiskScheduler.run(DiskScheduler.Algorithm.FCFS, disk, 2500, 4999, DiskScheduler.Direction.UP, true).totalMovement());

        List<Fragmentation.Request> reqs = new ArrayList<>();
        List<Integer> blocks = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            blocks.add(50 + rnd.nextInt(950));
            reqs.add(new Fragmentation.Request("P" + i, 10 + rnd.nextInt(400)));
        }
        for (Fragmentation.Strategy s : Fragmentation.Strategy.values()) {
            for (Fragmentation.Mode mode : Fragmentation.Mode.values()) {
                Fragmentation.Result r = Fragmentation.run(s, mode, blocks, reqs);
                check("Large Frag " + s + "/" + mode + " (2000x2000) keeps every process accounted for", 2000, r.allocations().size());
            }
        }

        List<Multiprogramming.Job> jobs = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            jobs.add(new Multiprogramming.Job("J" + i, rnd.nextInt(2000), 5 + rnd.nextInt(60), 1 + rnd.nextInt(20)));
        }
        Multiprogramming.Result mr = Multiprogramming.run(jobs, 256, 4);
        check("Large Multi (1000 jobs): all complete", 1000, mr.jobs().size());
        check("Large Multi: utilization <= 100%", true, mr.cpuUtilization() <= 100.0 && mr.avgMemoryUtilization() <= 100.0);

        List<Paging.Entry> table = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            table.add(new Paging.Entry(i, i));
        }
        List<Long> addrs = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            addrs.add((long) rnd.nextInt(5000 * 4096));
        }
        check("Large Paging: 20000 translations", 20000, Paging.translate(4096, 5000, table, addrs).size());
    }

    // ---------------------------------------------------------------- helpers
    private static void check(String name, Object expected, Object actual) {
        boolean ok;
        if (expected instanceof Double e && actual instanceof Double a) {
            ok = Math.abs(e - a) < 1e-9;
        } else if (expected instanceof Number e && actual instanceof Number a) {
            ok = e.longValue() == a.longValue();
        } else {
            ok = expected == null ? actual == null : expected.equals(actual);
        }
        record(ok, name, expected, actual);
    }

    private static void expectInvalid(String name, Runnable action) {
        boolean ok = false;
        try {
            action.run();
        } catch (InputException e) {
            ok = true;
        } catch (RuntimeException e) {
            // wrong exception type = failure
        }
        record(ok, name, "InputException", ok ? "InputException" : "none/other");
    }

    private static void record(boolean ok, String name, Object expected, Object actual) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + "  expected=" + expected + " actual=" + actual);
        }
    }
}
