package dev.amazepr.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.ide.util.PropertiesComponent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;

final class ManifestWalkthroughView {
    private final Project project;
    private final ManifestWalkthroughLoader loader = new ManifestWalkthroughLoader();
    private final ManifestJsonParser manifestParser = new ManifestJsonParser();
    private final ManifestDiffOpener diffOpener;
    private final JPanel root = new JPanel(new BorderLayout());
    private final JLabel status = new JLabel();
    private final JPanel content = new JPanel(new BorderLayout());
    private final JTextArea rawText = new JTextArea();
    private final StepRail stepper = new StepRail();
    private final JLabel walkthroughTitle = new JLabel("PR Walkthrough");
    private final JLabel stepTitle = new JLabel();
    private final JTextArea whatChanged = sectionText();
    private final JTextArea why = sectionText();
    private final JTextArea workflowEffect = sectionText();
    private final JScrollPane explanationScroll = new JScrollPane(
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JPanel fileActions = new JPanel();
    private final JButton previous = new JButton("Previous");
    private final JButton openCurrent = new JButton("Open diff");
    private final JButton next = new JButton("Next");
    private final JComboBox<String> comparisonMode = new JComboBox<>(new String[] {"Working tree", "Pull request"});
    private final JTextField baseBranch = new JTextField(18);
    private ViewMode currentMode = ViewMode.WALKTHROUGH;
    private ParsedWalkthrough parsedWalkthrough = new ParsedWalkthrough("", "", List.of(), List.of());
    private int stepIndex = 0;

    ManifestWalkthroughView(Project project) {
        this.project = project;
        this.diffOpener = new ManifestDiffOpener(project);
        configure();
        reload();
    }

    JComponent component() {
        return root;
    }

    private void configure() {
        PropertiesComponent settings = PropertiesComponent.getInstance(project);
        comparisonMode.setSelectedIndex(settings.getBoolean("amazepr.diff.pullRequest", false) ? 1 : 0);
        baseBranch.setText(settings.getValue("amazepr.diff.base", "origin/main"));
        baseBranch.setEnabled(comparisonMode.getSelectedIndex() == 1);
        comparisonMode.setToolTipText("Working tree: HEAD versus local files. Pull request: merge base versus committed HEAD.");
        baseBranch.setToolTipText("Local Git reference for the target branch, for example origin/main. Fetch the branch before comparing.");
        comparisonMode.addActionListener(event -> {
            boolean pr = comparisonMode.getSelectedIndex() == 1;
            baseBranch.setEnabled(pr);
            settings.setValue("amazepr.diff.pullRequest", pr);
        });
        JPanel comparison = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        comparison.add(new JLabel("Compare"));
        comparison.add(comparisonMode);
        comparison.add(new JLabel("Base branch"));
        comparison.add(baseBranch);

        JButton walkthrough = headerButton("Walkthrough");
        walkthrough.addActionListener(event -> {
            currentMode = ViewMode.WALKTHROUGH;
            reload();
        });

        JButton manifest = headerButton("Manifest");
        manifest.addActionListener(event -> {
            currentMode = ViewMode.MANIFEST;
            reload();
        });

        JButton reload = headerButton("Reload");
        reload.addActionListener(event -> reload());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(walkthrough);
        actions.add(manifest);
        actions.add(reload);

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        header.add(status, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);

        rawText.setEditable(false);
        rawText.setLineWrap(true);
        rawText.setWrapStyleWord(true);
        rawText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rawText.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        previous.addActionListener(event -> {
            if (stepIndex > 0) {
                stepIndex--;
                renderCurrentStep();
            }
        });
        openCurrent.addActionListener(event -> openCurrentStepDiff());
        next.addActionListener(event -> {
            if (stepIndex < parsedWalkthrough.steps().size() - 1) {
                stepIndex++;
                renderCurrentStep();
            }
        });

        root.setBorder(BorderFactory.createEmptyBorder());
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.add(header, BorderLayout.NORTH);
        toolbar.add(comparison, BorderLayout.SOUTH);
        root.add(toolbar, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
    }

    private void reload() {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            status.setText("No project path is available.");
            showRawText("Open a project with a filesystem path to use aMazePR.");
            return;
        }

        WalkthroughContent content = switch (currentMode) {
            case WALKTHROUGH -> loader.loadManifestOnly(Path.of(basePath));
            case MANIFEST -> loader.loadManifestOnly(Path.of(basePath));
        };
        status.setText(content.status());
        if (currentMode == ViewMode.WALKTHROUGH) {
            parsedWalkthrough = manifestParser.parse(content.body());
            stepIndex = 0;
            renderWalkthrough();
        } else {
            showRawText(content.body());
        }
    }

    private void renderWalkthrough() {
        content.removeAll();

        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        walkthroughTitle.setText("PR Walkthrough");
        walkthroughTitle.setFont(walkthroughTitle.getFont().deriveFont(Font.BOLD, 17));
        container.add(walkthroughTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        stepper.setPreferredSize(new Dimension(0, 46));
        body.add(stepper, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.add(diagnosticsPanel(), BorderLayout.NORTH);
        explanationScroll.setViewportView(stepCard());
        explanationScroll.setBorder(BorderFactory.createEmptyBorder());
        explanationScroll.setMinimumSize(new Dimension(0, 0));
        center.add(explanationScroll, BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 8));
        navigation.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        styleSecondaryButton(previous);
        stylePrimaryButton(openCurrent);
        stylePrimaryButton(next);
        navigation.add(previous);
        navigation.add(openCurrent);
        navigation.add(next);
        body.add(navigation, BorderLayout.SOUTH);

        container.add(body, BorderLayout.CENTER);
        content.add(container, BorderLayout.CENTER);
        renderCurrentStep();
        content.revalidate();
        content.repaint();
    }

    private JPanel stepCard() {
        JPanel card = new StepCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(78, 88, 104)),
            BorderFactory.createEmptyBorder(18, 18, 16, 18)
        ));

        stepTitle.setFont(stepTitle.getFont().deriveFont(Font.BOLD, 20));
        stepTitle.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        card.add(stepTitle);
        card.add(Box.createVerticalStrut(14));
        card.add(sectionLabel("WHAT CHANGED"));
        whatChanged.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        card.add(whatChanged);
        card.add(Box.createVerticalStrut(12));
        card.add(sectionLabel("WHY"));
        why.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        card.add(why);
        card.add(Box.createVerticalStrut(12));
        card.add(sectionLabel("WORKFLOW EFFECT"));
        workflowEffect.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        card.add(workflowEffect);
        card.add(Box.createVerticalStrut(12));
        card.add(fileActions);
        return card;
    }

    private JComponent diagnosticsPanel() {
        List<String> diagnostics = parsedWalkthrough.diagnostics();
        if (diagnostics.isEmpty()) {
            JLabel ok = new JLabel("Manifest verified by plugin parser.");
            ok.setForeground(new Color(86, 156, 100));
            return ok;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(156, 122, 46)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        JLabel title = sectionLabel("DIAGNOSTICS");
        panel.add(title);
        for (String diagnostic : diagnostics) {
            JLabel item = new JLabel("- " + diagnostic);
            panel.add(item);
        }
        return panel;
    }

    private void renderCurrentStep() {
        List<WalkthroughStep> steps = parsedWalkthrough.steps();
        if (steps.isEmpty()) {
            showRawText("No walkthrough steps found.");
            return;
        }

        WalkthroughStep step = steps.get(stepIndex);
        status.setText(parsedWalkthrough.title() + "  •  Step " + (stepIndex + 1) + " of " + steps.size());
        stepTitle.setText(step.title());
        whatChanged.setText(step.description().isBlank() ? parsedWalkthrough.intent() : step.description());
        why.setText(parsedWalkthrough.intent().isBlank() ? "Review this step in the generated manifest." : parsedWalkthrough.intent());
        workflowEffect.setText(step.files().isEmpty()
            ? "This step has no file references."
            : "Review " + step.files().size() + " referenced file target(s).");

        fileActions.removeAll();
        fileActions.setLayout(new BoxLayout(fileActions, BoxLayout.Y_AXIS));
        fileActions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        for (int fileIndex = 0; fileIndex < step.files().size(); fileIndex++) {
            WorkflowFileReference reference = step.files().get(fileIndex);
            int selectedIndex = fileIndex;
            JButton file = new JButton("Open diff  •  " + reference.displayLabel());
            file.setHorizontalAlignment(SwingConstants.LEFT);
            file.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            styleFileButton(file);
            file.addActionListener(event -> openStepDiff(selectedIndex));
            fileActions.add(file);
            fileActions.add(Box.createVerticalStrut(6));
        }

        previous.setEnabled(stepIndex > 0);
        openCurrent.setEnabled(!step.files().isEmpty());
        openCurrent.setText(step.files().isEmpty() ? "No file comparison" : "Open step diff • " + step.files().size() + " files");
        next.setEnabled(stepIndex < steps.size() - 1);
        next.setText(stepIndex < steps.size() - 1 ? "Next step" : "Done");

        stepper.setState(steps.size(), stepIndex, selectedIndex -> {
            stepIndex = selectedIndex;
            renderCurrentStep();
        });
        content.revalidate();
        content.repaint();
        SwingUtilities.invokeLater(() -> explanationScroll.getViewport().setViewPosition(new Point(0, 0)));
    }

    private void showRawText(String body) {
        content.removeAll();
        rawText.setText(body);
        rawText.setCaretPosition(0);
        content.add(new JScrollPane(rawText), BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(104, 164, 255));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11));
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return label;
    }

    private static JTextArea sectionText() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        return area;
    }

    private static JButton headerButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        return button;
    }

    private static void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private static void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private static void styleFileButton(JButton button) {
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private void openStepDiff(int selectedIndex) {
        List<WorkflowFileReference> files = currentStepFiles();
        if (files.isEmpty()) return;
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            status.setText("No project path is available.");
            return;
        }
        status.setText("Opening step comparison for " + files.size() + " files...");
        String base = baseBranch.getText().strip();
        PropertiesComponent.getInstance(project).setValue("amazepr.diff.base", base);
        diffOpener.open(Path.of(basePath), files, selectedIndex, comparisonMode.getSelectedIndex() == 1, base, status::setText);
    }

    private void openCurrentStepDiff() {
        openStepDiff(0);
    }

    private List<WorkflowFileReference> currentStepFiles() {
        List<WalkthroughStep> steps = parsedWalkthrough.steps();
        if (stepIndex < 0 || stepIndex >= steps.size()) {
            return List.of();
        }

        return steps.get(stepIndex).files();
    }

    private enum ViewMode {
        WALKTHROUGH,
        MANIFEST
    }

    /** Follow the viewport width so wrapped prose grows vertically instead of being clipped. */
    private static final class StepCard extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(20, visible.height - 20);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private interface StepSelection {
        void select(int index);
    }

    private static final class StepRail extends JPanel {
        private int count = 0;
        private int selected = 0;
        private StepSelection selection = index -> {
        };

        StepRail() {
            setLayout(null);
            setOpaque(false);
        }

        void setState(int count, int selected, StepSelection selection) {
            this.count = count;
            this.selected = selected;
            this.selection = selection;
            removeAll();
            for (int index = 0; index < count; index++) {
                JButton button = circleButton(index + 1, index == selected);
                int selectedIndex = index;
                button.addActionListener(event -> this.selection.select(selectedIndex));
                add(button);
            }
            revalidate();
            repaint();
        }

        @Override
        public void doLayout() {
            int buttonSize = 34;
            int width = Math.max(getWidth(), buttonSize * Math.max(count, 1));
            int y = 6;
            for (int index = 0; index < getComponentCount(); index++) {
                int x = positionFor(index, width, buttonSize);
                getComponent(index).setBounds(x, y, buttonSize, buttonSize);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (count <= 1) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(117, 126, 140));
            int buttonSize = 34;
            int y = 23;
            int width = Math.max(getWidth(), buttonSize * count);
            for (int index = 0; index < count - 1; index++) {
                int x1 = positionFor(index, width, buttonSize) + buttonSize;
                int x2 = positionFor(index + 1, width, buttonSize);
                g.drawLine(x1, y, x2, y);
            }
            g.dispose();
        }

        private int positionFor(int index, int width, int buttonSize) {
            if (count <= 1) {
                return (width - buttonSize) / 2;
            }
            int available = width - buttonSize;
            return (available * index) / (count - 1);
        }

        private JButton circleButton(int number, boolean selected) {
            JButton button = new JButton(Integer.toString(number));
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createLineBorder(new Color(130, 139, 153)));
            button.setForeground(selected ? Color.WHITE : new Color(210, 216, 224));
            button.setBackground(selected ? new Color(53, 116, 240) : new Color(43, 47, 54));
            return button;
        }
    }
}
