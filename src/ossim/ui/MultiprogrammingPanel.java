package ossim.ui;

import ossim.core.Multiprogramming;
import ossim.util.InputException;
import ossim.util.Parse;

import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 8: multiprogramming. */
final class MultiprogrammingPanel extends ModulePanel {
    private final InputTable programs = new InputTable("Programs", "Program", "Arrival", "Memory", "CPU Burst");
    private final JTextField totalMemory = new JTextField("100", 6);
    private final JTextField quantum = new JTextField("4", 6);

    MultiprogrammingPanel() {
        super("<b>Multiprogramming</b> - several programs share main memory and the CPU. Programs are admitted first-come "
                + "first-served while memory allows (a program that does not fit blocks the queue), and resident programs "
                + "share the CPU by Round Robin. The timeline shows memory use, resident programs and the waiting queue; "
                + "the summary reports CPU/memory utilization and the degree of multiprogramming.");
        arrange(programs,
                Ui.flow(new javax.swing.JLabel("Total memory:"), totalMemory, new javax.swing.JLabel("Time quantum:"), quantum),
                Ui.flow(action("Load Sample", this::loadSample), action("Load Large (300)", this::loadLarge),
                        action("Run", this::run)));
        loadSample();
    }

    private void loadSample() {
        totalMemory.setText("100");
        quantum.setText("4");
        programs.setRows(List.of(new String[]{"J1", "0", "40", "10"}, new String[]{"J2", "1", "30", "8"},
                new String[]{"J3", "2", "50", "6"}, new String[]{"J4", "3", "20", "5"}, new String[]{"J5", "4", "60", "7"}));
    }

    private void loadLarge() {
        Random rnd = new Random();
        totalMemory.setText("256");
        quantum.setText("4");
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            rows.add(new String[]{"J" + i, String.valueOf(rnd.nextInt(600)), String.valueOf(5 + rnd.nextInt(60)),
                    String.valueOf(1 + rnd.nextInt(20))});
        }
        programs.setRows(rows);
    }

    private void run() {
        int memory = Parse.integer(totalMemory.getText(), "Total memory", 1, Ui.MAX_VALUE);
        int q = Parse.integer(quantum.getText(), "Time quantum", 1, Ui.MAX_VALUE);
        List<Multiprogramming.Job> jobs = new ArrayList<>();
        List<String[]> rows = programs.rows();
        for (int i = 0; i < rows.size(); i++) {
            String[] c = rows.get(i);
            String row = "Row " + (i + 1) + " ";
            if (c[0].isEmpty()) {
                throw new InputException(row + "program name is blank.");
            }
            jobs.add(new Multiprogramming.Job(c[0], Parse.integer(c[1], row + "arrival", 0, Ui.MAX_VALUE),
                    Parse.integer(c[2], row + "memory", 1, Ui.MAX_VALUE), Parse.integer(c[3], row + "CPU burst", 1, Ui.MAX_VALUE)));
        }
        Multiprogramming.Result r = Multiprogramming.run(jobs, memory, q);

        output.reset();
        output.addTable("System Timeline", new String[]{"Start", "End", "Running", "Resident (degree)", "Memory used",
                "Memory %", "Waiting for memory"}, timelineRows(r));
        output.addTable("Programs", new String[]{"Program", "Arrival", "Memory", "Burst", "Admitted", "Completion",
                "Turnaround", "Waiting", "Admission delay"}, jobRows(r));
        output.setSummary("Total memory: " + r.totalMemory() + "   Quantum: " + q + "   Programs: " + r.jobs().size() + "\n"
                + "Finish time: " + r.finish() + "\n"
                + "CPU utilization    : " + Ui.two(r.cpuUtilization()) + " %\n"
                + "Memory utilization : " + Ui.two(r.avgMemoryUtilization()) + " % (time-weighted average)\n"
                + "Max degree of multiprogramming: " + r.maxDegree() + " programs in memory at once\n"
                + "Throughput: " + String.format("%.4f", r.throughput()) + " programs per time unit\n"
                + "Waiting = Turnaround - Burst; Admission delay = time spent waiting for memory.");
    }

    private static List<Object[]> timelineRows(Multiprogramming.Result r) {
        List<Object[]> rows = new ArrayList<>();
        for (Multiprogramming.Snapshot s : r.timeline()) {
            rows.add(new Object[]{s.start(), s.end(), s.running(),
                    s.resident().isEmpty() ? "-" : s.resident().size() + ": " + String.join(", ", s.resident()),
                    s.memoryUsed() + " / " + r.totalMemory(), Ui.two(s.memoryUsed() * 100.0 / r.totalMemory()),
                    s.waiting().isEmpty() ? "-" : String.join(", ", s.waiting())});
        }
        return rows;
    }

    private static List<Object[]> jobRows(Multiprogramming.Result r) {
        List<Object[]> rows = new ArrayList<>();
        for (Multiprogramming.JobStat s : r.jobs()) {
            rows.add(new Object[]{s.job().id(), s.job().arrival(), s.job().memory(), s.job().burst(), s.admitted(),
                    s.completion(), s.turnaround(), s.waiting(), s.admitted() - s.job().arrival()});
        }
        return rows;
    }
}
