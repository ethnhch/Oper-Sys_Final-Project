package ossim.ui;

import ossim.core.CpuScheduler;
import ossim.core.CpuScheduler.Algorithm;
import ossim.core.CpuScheduler.Proc;
import ossim.core.CpuScheduler.Result;
import ossim.util.InputException;
import ossim.util.Parse;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 1: CPU scheduling. */
final class CpuPanel extends ModulePanel {
    private static final String[] RESULT_COLUMNS =
            {"PID", "Arrival", "Burst", "Priority", "Completion", "Turnaround", "Waiting", "Response"};
    private static final String[] COMPARE_COLUMNS =
            {"Algorithm", "Avg Waiting", "Avg Turnaround", "Avg Response", "CPU Util %"};

    private record Input(List<Proc> procs, boolean hasPriority) { }

    private final InputTable processes = table(new InputTable("Processes", "PID", "Arrival", "Burst", "Priority"), "Row ");
    private final JComboBox<Algorithm> algorithm = new JComboBox<>(Algorithm.values());
    private final JTextField quantum = field("Time quantum", "4", 5);

    CpuPanel() {
        super("CPU Scheduling", Icons.Kind.CPU, "Processes",
                "Decide which ready process runs next. FCFS, SJF, SRTF, Priority (both modes) and Round Robin are "
                + "supported and every run also compares all algorithms. A lower priority number means a higher priority; "
                + "leave Priority blank when it is not used.");
        algorithm.addActionListener(e -> quantum.setEnabled(algorithm.getSelectedItem() == Algorithm.ROUND_ROBIN));
        arrange(processes, Ui.row(Ui.form("Algorithm", algorithm), Ui.form("Time quantum (RR)", quantum)));
        actions(this::loadSample, "Load Large (1000)", this::loadLarge, this::run);
        quantum.setEnabled(false);
        loadSample();
    }

    private void loadSample() {
        processes.setRows(List.of(new String[]{"P1", "0", "8", "3"}, new String[]{"P2", "1", "4", "1"},
                new String[]{"P3", "2", "9", "4"}, new String[]{"P4", "3", "5", "2"}));
    }

    private void loadLarge() {
        Random rnd = new Random();
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            rows.add(new String[]{"P" + i, String.valueOf(rnd.nextInt(2000)),
                    String.valueOf(1 + rnd.nextInt(20)), String.valueOf(1 + rnd.nextInt(10))});
        }
        processes.setRows(rows);
    }

    private Input readProcesses() {
        List<String[]> rows = processes.rows();
        List<Proc> procs = new ArrayList<>(rows.size());
        boolean hasPriority = true;
        for (int i = 0; i < rows.size(); i++) {
            String[] c = rows.get(i);
            String row = "Row " + (i + 1) + " ";
            if (c[0].isEmpty()) {
                throw new InputException(row + "PID is blank.");
            }
            int arrival = Parse.integer(c[1], row + "Arrival", 0, Ui.MAX_VALUE);
            int burst = Parse.integer(c[2], row + "Burst", 1, Ui.MAX_VALUE);
            hasPriority &= !c[3].isEmpty();
            int priority = c[3].isEmpty() ? 0 : Parse.integer(c[3], row + "Priority", 0, Ui.MAX_VALUE);
            procs.add(new Proc(c[0], arrival, burst, priority));
        }
        return new Input(procs, hasPriority);
    }

    private void run() {
        Algorithm selected = (Algorithm) algorithm.getSelectedItem();
        Input input = readProcesses();
        int q = Parse.integer(quantum.getText(), "Time quantum", 1, Ui.MAX_VALUE);
        if (selected.usesPriority() && !input.hasPriority()) {
            throw new InputException("Fill in the Priority column for every process to use " + selected + ".");
        }
        Result result = CpuScheduler.run(selected, input.procs(), q);

        List<Object[]> comparison = comparisonRows(input, q);
        output.reset();
        output.addStat("Avg waiting", Ui.two(result.avgWaiting()), Theme.Tone.ACCENT, "time units");
        output.addStat("Avg turnaround", Ui.two(result.avgTurnaround()), Theme.Tone.NEUTRAL, "time units");
        output.addStat("Avg response", Ui.two(result.avgResponse()), Theme.Tone.NEUTRAL, "time units");
        output.addStat("CPU utilization", Ui.two(result.cpuUtilization()) + " %",
                result.cpuUtilization() >= 99.99 ? Theme.Tone.SUCCESS : Theme.Tone.WARNING, result.rows().size() + " processes");
        output.addComponent("Gantt Chart", new GanttChart(result.gantt()));
        output.addTable("Per-process Results", RESULT_COLUMNS, resultRows(result, input.hasPriority()));
        List<String> names = new ArrayList<>();
        List<Double> waits = new ArrayList<>();
        for (Object[] row : comparison) {
            names.add((String) row[0]);
            waits.add(Double.parseDouble((String) row[1]));
        }
        output.addChartAndTable("Compare Algorithms", new BarChart("Average waiting time", names, waits, true,
                selected.toString()), COMPARE_COLUMNS, comparison);
        output.setSummary(summary(result, q, input));
    }

    private static List<Object[]> resultRows(Result result, boolean hasPriority) {
        List<Object[]> rows = new ArrayList<>();
        for (CpuScheduler.Row r : result.rows()) {
            rows.add(new Object[]{r.proc().id(), r.proc().arrival(), r.proc().burst(),
                    hasPriority ? r.proc().priority() : "-", r.completion(), r.turnaround(), r.waiting(), r.response()});
        }
        return rows;
    }

    private static List<Object[]> comparisonRows(Input input, int quantum) {
        List<Object[]> rows = new ArrayList<>();
        for (Algorithm a : Algorithm.values()) {
            if (a.usesPriority() && !input.hasPriority()) {
                continue;
            }
            Result r = CpuScheduler.run(a, input.procs(), quantum);
            rows.add(new Object[]{a.toString(), Ui.two(r.avgWaiting()), Ui.two(r.avgTurnaround()),
                    Ui.two(r.avgResponse()), Ui.two(r.cpuUtilization())});
        }
        return rows;
    }

    private static String summary(Result r, int quantum, Input input) {
        List<String> order = new ArrayList<>();
        r.gantt().forEach(s -> order.add(s.id() + "[" + s.start() + "-" + s.end() + "]"));
        Algorithm best = null;
        double bestWait = Double.MAX_VALUE;
        for (Algorithm a : Algorithm.values()) {
            if (a.usesPriority() && !input.hasPriority()) {
                continue;
            }
            double wait = CpuScheduler.run(a, input.procs(), quantum).avgWaiting();
            if (wait < bestWait) {
                bestWait = wait;
                best = a;
            }
        }
        return "Algorithm: " + r.algorithm() + (r.algorithm() == Algorithm.ROUND_ROBIN ? " (quantum " + quantum + ")" : "") + "\n"
                + "Processes: " + r.rows().size() + "\n"
                + "Execution order: " + Ui.preview(order, 40, " ") + "\n"
                + "Average waiting time    : " + Ui.two(r.avgWaiting()) + "\n"
                + "Average turnaround time : " + Ui.two(r.avgTurnaround()) + "\n"
                + "Average response time   : " + Ui.two(r.avgResponse()) + "\n"
                + "CPU utilization         : " + Ui.two(r.cpuUtilization()) + " %\n"
                + "Turnaround = Completion - Arrival | Waiting = Turnaround - Burst | Response = First run - Arrival\n"
                + "Lowest average waiting time in the comparison: " + best + " (" + Ui.two(bestWait) + ")";
    }
}
