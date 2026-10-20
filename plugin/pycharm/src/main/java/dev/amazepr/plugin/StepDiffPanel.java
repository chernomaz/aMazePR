package dev.amazepr.plugin;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.IntConsumer;

/** Persistent step targets beside the native diff. Selection works independently of diff toolbar settings. */
final class StepDiffPanel extends JPanel {
    private final JList<String> files;
    private final JLabel count = new JLabel();
    private final JButton previous = new JButton("Previous file");
    private final JButton next = new JButton("Next file");
    private boolean syncing;

    StepDiffPanel(List<String> names, int selectedIndex, JComponent diff, IntConsumer onSelection) {
        super(new BorderLayout());
        if (names.isEmpty()) throw new IllegalArgumentException("A step must have file targets.");
        files = new JList<>(names.toArray(String[]::new));
        files.setName("amazepr.step.files");
        files.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        files.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                  boolean selected, boolean focused) {
                JComponent cell = (JComponent) super.getListCellRendererComponent(list, value, index, selected, focused);
                cell.setToolTipText(String.valueOf(value));
                return cell;
            }
        });
        files.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || files.getSelectedIndex() < 0) return;
            int index = files.getSelectedIndex();
            count.setText("Step files: " + (index + 1) + " of " + names.size());
            previous.setEnabled(index > 0);
            next.setEnabled(index + 1 < names.size());
            files.ensureIndexIsVisible(index);
            if (!syncing) onSelection.accept(index);
        });
        previous.addActionListener(event -> files.setSelectedIndex(files.getSelectedIndex() - 1));
        next.addActionListener(event -> files.setSelectedIndex(files.getSelectedIndex() + 1));
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        navigation.add(previous);
        navigation.add(next);
        JPanel sidebar = new JPanel(new BorderLayout(0, 8));
        sidebar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        sidebar.add(count, BorderLayout.NORTH);
        sidebar.add(new JScrollPane(files), BorderLayout.CENTER);
        sidebar.add(navigation, BorderLayout.SOUTH);
        sidebar.setMinimumSize(new Dimension(230, 0));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, diff);
        split.setResizeWeight(0.25);
        split.setDividerLocation(320);
        setPreferredSize(new Dimension(1200, 760));
        add(split, BorderLayout.CENTER);
        syncSelection(Math.max(0, Math.min(selectedIndex, names.size() - 1)));
    }

    void syncSelection(int index) {
        if (index < 0 || index >= files.getModel().getSize()) return;
        syncing = true;
        try {
            files.setSelectedIndex(index);
        } finally {
            syncing = false;
        }
    }
}
