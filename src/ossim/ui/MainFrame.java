package ossim.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main window: the dark sidebar is the main menu of the mini operating system; the work area shows either the
 * home overview or one module screen. Keyboard: Ctrl+0 home, Ctrl+1..8 modules, Ctrl+Enter run, Ctrl+D theme.
 */
public final class MainFrame extends JFrame {
    private static final String HOME = "home";

    private final CardLayout cards = new CardLayout();
    private final JPanel work = new JPanel(cards) {
        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.c(Theme.Role.BG));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    private final List<ModulePanel> modules = new ArrayList<>();
    private final Sidebar sidebar;
    private int current = -1;

    public MainFrame() {
        super("MiniOS · Operating System Simulator");
        modules.add(new CpuPanel());
        modules.add(new BankerPanel());
        modules.add(new PagingPanel());
        modules.add(new PageReplacementPanel());
        modules.add(new SegmentationPanel());
        modules.add(new FragmentationPanel());
        modules.add(new DiskPanel());
        modules.add(new MultiprogrammingPanel());

        String[] navLabels = {"CPU Scheduling", "Deadlock (Banker's)", "Paging", "Page Replacement", "Segmentation",
                "Fragmentation", "Disk Scheduling", "Multiprogramming"};
        String[] groups = {"Processes", "Processes", "Memory", "Memory", "Memory", "Memory", "Storage", "System"};
        List<Sidebar.Entry> entries = new ArrayList<>();
        entries.add(new Sidebar.Entry(-1, "Overview", Icons.Kind.HOME, null, "Ctrl+0"));
        for (int i = 0; i < modules.size(); i++) {
            ModulePanel m = modules.get(i);
            entries.add(new Sidebar.Entry(i, (i + 1) + ".  " + navLabels[i], m.icon, groups[i], "Ctrl+" + (i + 1)));
        }
        sidebar = new Sidebar(entries, this::show, this::toggleTheme);

        work.add(Ui.scroll(new HomePanel(homeCards(), this::show)), HOME);
        for (int i = 0; i < modules.size(); i++) {
            work.add(modules.get(i), String.valueOf(i));
        }

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(work, BorderLayout.CENTER);
        setContentPane(root);
        bindKeys(root);
        show(-1);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1480, screen.width - 40), Math.min(900, screen.height - 60));
        setLocationRelativeTo(null);
    }

    private List<HomePanel.ModuleInfo> homeCards() {
        String[] blurbs = {
                "Order processes on the CPU and measure waiting, turnaround and response time.",
                "Compute Need, run the safety algorithm, find a safe sequence and test new requests.",
                "Split logical addresses into page and offset and map them to physical frames.",
                "Track frames reference by reference and count hits and page faults.",
                "Translate (segment, offset) pairs with base and limit checks and trap invalid ones.",
                "Place processes into memory blocks and expose internal and external fragmentation.",
                "Order cylinder requests and measure total head movement.",
                "Share memory and the CPU between programs and report utilization over time."
        };
        List<List<String>> algorithms = List.of(
                List.of("FCFS", "SJF", "SRTF", "Priority", "RR"),
                List.of("Need", "Safety", "Request"),
                List.of("Page table", "Address translation"),
                List.of("FIFO", "LRU", "Optimal", "Clock"),
                List.of("Base / limit", "Traps"),
                List.of("First", "Best", "Worst", "Next fit"),
                List.of("FCFS", "SSTF", "SCAN", "C-SCAN", "LOOK", "C-LOOK"),
                List.of("Admission", "Round Robin", "Utilization"));
        List<HomePanel.ModuleInfo> out = new ArrayList<>();
        for (int i = 0; i < modules.size(); i++) {
            ModulePanel m = modules.get(i);
            out.add(new HomePanel.ModuleInfo(String.valueOf(i + 1), m.title, m.icon, m.category, blurbs[i], algorithms.get(i)));
        }
        return out;
    }

    private void show(int index) {
        current = index;
        cards.show(work, index < 0 ? HOME : String.valueOf(index));
        sidebar.select(index);
        setTitle(index < 0 ? "MiniOS · Operating System Simulator" : "MiniOS · " + modules.get(index).title);
    }

    private void toggleTheme() {
        Theme.setDark(!Theme.isDark());
        sidebar.themeChanged(Theme.isDark());
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    private void bindKeys(JComponent root) {
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_0, menu), "home", () -> show(-1));
        for (int i = 0; i < modules.size(); i++) {
            int index = i;
            bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, menu), "module" + i, () -> show(index));
        }
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menu), "run", () -> {
            if (current >= 0 && modules.get(current).runButton() != null) {
                modules.get(current).runButton().doClick();
            }
        });
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_D, menu), "theme", this::toggleTheme);
    }

    private static void bind(JComponent root, KeyStroke key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }
}
