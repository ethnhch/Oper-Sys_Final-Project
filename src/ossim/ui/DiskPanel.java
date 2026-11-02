package ossim.ui;

import ossim.core.DiskScheduler;
import ossim.core.DiskScheduler.Algorithm;
import ossim.core.DiskScheduler.Direction;
import ossim.core.DiskScheduler.Result;
import ossim.util.Parse;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 7: disk scheduling. */
final class DiskPanel extends ModulePanel {
    private final JTextArea requests = Ui.textArea(8, "");
    private final JTextField head = field("Initial head position", "53", 6);
    private final JTextField maxCylinder = field("Highest cylinder", "199", 6);
    private final JComboBox<Algorithm> algorithm = new JComboBox<>(Algorithm.values());
    private final JComboBox<Direction> direction = new JComboBox<>(Direction.values());
    private final JCheckBox countJump = new JCheckBox("Count C-SCAN / C-LOOK return jump", true);

    DiskPanel() {
        super("Disk Scheduling", Icons.Kind.DISK, "Storage",
                "Order pending cylinder requests to reduce how far the disk head travels. FCFS, SSTF, SCAN, C-SCAN, LOOK "
                + "and C-LOOK are supported. SCAN and C-SCAN go to the disk edge, LOOK and C-LOOK turn at the last "
                + "request. Every run also compares all algorithms.");
        algorithm.addActionListener(e -> updateOptions());
        arrange(listInput("Cylinder requests", "Cylinder numbers separated by commas or spaces", requests, "Cylinder request"),
                Ui.row(Ui.form("Initial head", head), Ui.form("Highest cylinder", maxCylinder)),
                Ui.row(Ui.form("Algorithm", algorithm), Ui.form("Direction", direction)),
                Ui.row(countJump));
        actions(this::loadSample, "Load Large (5000)", this::loadLarge, this::run);
        updateOptions();
        loadSample();
    }

    /** Direction only matters for the sweeping algorithms, the jump option only for the circular ones. */
    private void updateOptions() {
        Algorithm a = (Algorithm) algorithm.getSelectedItem();
        direction.setEnabled(a.usesDirection());
        countJump.setEnabled(a == Algorithm.C_SCAN || a == Algorithm.C_LOOK);
    }

    private void loadSample() {
        requests.setText("98, 183, 37, 122, 14, 124, 65, 67");
        head.setText("53");
        maxCylinder.setText("199");
    }

    private void loadLarge() {
        Random rnd = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            text.append(rnd.nextInt(5000)).append(i < 4999 ? ", " : "");
        }
        requests.setText(text.toString());
        head.setText("2500");
        maxCylinder.setText("4999");
    }

    private void run() {
        int max = Parse.integer(maxCylinder.getText(), "Highest cylinder", 1, Ui.MAX_VALUE);
        int start = Parse.integer(head.getText(), "Initial head position", 0, max);
        List<Integer> list = Parse.integerList(requests.getText(), "Cylinder request", 0, max);
        Algorithm selected = (Algorithm) algorithm.getSelectedItem();
        Direction dir = (Direction) direction.getSelectedItem();
        boolean jump = countJump.isSelected();
        Result result = DiskScheduler.run(selected, list, start, max, dir, jump);

        List<Object[]> comparison = comparisonRows(list, start, max, dir, jump);
        List<String> names = new ArrayList<>();
        List<Double> totals = new ArrayList<>();
        String best = "";
        double bestValue = Double.MAX_VALUE;
        for (Object[] row : comparison) {
            names.add((String) row[0]);
            totals.add(((Integer) row[1]).doubleValue());
            if ((Integer) row[1] < bestValue) {
                bestValue = (Integer) row[1];
                best = (String) row[0];
            }
        }
        output.reset();
        output.addStat("Head movement", Ui.grouped(result.totalMovement()), Theme.Tone.ACCENT, "cylinders");
        output.addStat("Avg / request", Ui.two((double) result.totalMovement() / list.size()), Theme.Tone.NEUTRAL,
                "cylinders");
        output.addStat("Requests", Ui.grouped(list.size()), Theme.Tone.NEUTRAL, "disk 0.." + max);
        output.addStat("Best algorithm", best, Theme.Tone.SUCCESS, Ui.grouped((long) bestValue) + " cylinders");
        output.addComponent("Head Movement", new HeadPathChart(start, result.moves(), max));
        output.addTable("Service Order", new String[]{"Step", "From", "To", "Distance", "Counted", "Cumulative", "Action"},
                moveRows(result));
        output.addChartAndTable("Compare Algorithms", new BarChart("Total head movement", names, totals, true,
                selected.toString()), new String[]{"Algorithm", "Total head movement", "Avg per request"}, comparison);
        List<String> order = new ArrayList<>();
        result.serviceOrder().forEach(c -> order.add(String.valueOf(c)));
        output.setSummary("Algorithm: " + selected + (selected.usesDirection() ? " (" + dir + ")" : "") + "\n"
                + "Requests: " + list.size() + "   Initial head: " + start + "   Disk: 0.." + max + "\n"
                + "Service order: " + Ui.preview(order, 40, " -> ") + "\n"
                + "Total head movement: " + result.totalMovement() + " cylinders   Average per request: "
                + Ui.two((double) result.totalMovement() / list.size()));
    }

    private static List<Object[]> moveRows(Result result) {
        List<Object[]> rows = new ArrayList<>();
        long cumulative = 0;
        int step = 1;
        for (DiskScheduler.Move m : result.moves()) {
            boolean counted = m.kind() != DiskScheduler.Kind.JUMP || result.jumpCounted();
            cumulative += counted ? m.distance() : 0;
            String action = switch (m.kind()) {
                case SERVE -> "Serve request";
                case EDGE -> "Travel to disk edge (no request)";
                case JUMP -> "Return jump";
            };
            rows.add(new Object[]{step++, m.from(), m.to(), m.distance(), counted ? "yes" : "no", cumulative, action});
        }
        return rows;
    }

    private static List<Object[]> comparisonRows(List<Integer> list, int start, int max, Direction dir, boolean jump) {
        List<Object[]> rows = new ArrayList<>();
        for (Algorithm a : Algorithm.values()) {
            int total = DiskScheduler.run(a, list, start, max, dir, jump).totalMovement();
            rows.add(new Object[]{a.toString(), total, Ui.two((double) total / list.size())});
        }
        return rows;
    }
}
