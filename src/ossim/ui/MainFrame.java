package ossim.ui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.Dimension;

/** Main window: the left-hand tab strip is the "main menu" of the mini operating system. */
public final class MainFrame extends JFrame {

    public MainFrame() {
        super("Mini Operating System Simulator");
        JTabbedPane menu = new JTabbedPane(JTabbedPane.LEFT);
        menu.addTab("1. CPU Scheduling", new CpuPanel());
        menu.addTab("2. Deadlock (Banker's)", new BankerPanel());
        menu.addTab("3. Paging", new PagingPanel());
        menu.addTab("4. Page Replacement", new PageReplacementPanel());
        menu.addTab("5. Segmentation", new SegmentationPanel());
        menu.addTab("6. Fragmentation", new FragmentationPanel());
        menu.addTab("7. Disk Scheduling", new DiskPanel());
        menu.addTab("8. Multiprogramming", new MultiprogrammingPanel());
        setContentPane(menu);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setSize(1360, 800);
        setLocationRelativeTo(null);
    }
}
