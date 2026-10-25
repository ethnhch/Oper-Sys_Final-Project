package ossim.ui;

import ossim.core.DiskScheduler;
import ossim.core.DiskScheduler.Algorithm;
import ossim.core.DiskScheduler.Direction;
import ossim.core.DiskScheduler.Result;
import ossim.util.Parse;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 7: disk scheduling. */
final class DiskPanel extends ModulePanel {
    private final JTextArea requests = Ui.textArea(8, "");
    private final JTextField head = new JTextField("53", 6);
    private final JTextField maxCylinder = new JTextField("199", 6);
    private final JComboBox<Algorithm> algorithm = new JComboBox<>(Algorithm.values());
    private final JComboBox<Direction> direction = new JComboBox<>(Direction.values());
    private final JCheckBox countJump = new JCheckBox("Count C-SCAN / C-LOOK return jump", true);

    DiskPanel() {
        super("<b>Disk Scheduling</b> - FCFS, SSTF, SCAN, C-SCAN, LOOK and C-LOOK. Enter any number of cylinder requests, "
                + "the initial head position and the highest cylinder. SCAN/C-SCAN go to the disk edge, LOOK/C-LOOK turn at "
                + "the last request. All algorithms are compared on each run.");
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createTitledBorder("Cylinder requests (numbers separated by commas or spaces)"));
        box.add(Ui.scroll(requests), BorderLayout.CENTER);
        arrange(box,
                Ui.flow(new JLabel("Head:"), head, new JLabel("Highest cylinder:"), maxCylinder),
                Ui.flow(new JLabel("Algorithm:"), algorithm, new JLabel("Direction:"), direction),
                Ui.flow(countJump),
                Ui.flow(action("Load Sample", this::loadSample), action("Load Large (5000)", this::loadLarge),
                        action("Run", this::run)));
        loadSample();
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

        output.reset();
        output.addTable("Service Order", new String[]{"Step", "From", "To", "Distance", "Counted", "Cumulative", "Action"},
                moveRows(result));
        output.addComponent("Head Movement Chart", new HeadPathChart(start, result.moves(), max));
        output.addTable("Comparison", new String[]{"Algorithm", "Total head movement", "Avg per request"},
                comparisonRows(list, start, max, dir, jump));
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
