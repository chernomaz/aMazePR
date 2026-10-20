package dev.amazepr.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Dependency-free Git integration checks, run by Gradle check / diffNavigationTest. */
public final class DiffNavigationTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("amazepr-diff-test-").toRealPath();
        try {
            stepChains(root);
            sidebarNavigation(root);
            git(root, "init", "-b", "main");
            git(root, "config", "user.name", "Diff test");
            git(root, "config", "user.email", "diff-test@example.invalid");
            git(root, "config", "core.autocrlf", "false");
            write(root, "src/file.txt", "original\n");
            write(root, "deleted.txt", "deleted content\n");
            write(root, "old name.txt", "renamed content\n");
            write(root, "empty.txt", "");
            commit(root, "base");
            git(root, "checkout", "-b", "feature");
            write(root, "src/file.txt", "feature\nsecond\nthird\n");
            write(root, "added.txt", "added\n");
            Files.delete(root.resolve("deleted.txt"));
            git(root, "mv", "old name.txt", "new name.txt");
            commit(root, "feature");
            git(root, "checkout", "main");
            write(root, "src/file.txt", "base-only edit\n");
            commit(root, "base advanced");
            git(root, "checkout", "feature");
            write(root, "src/file.txt", "uncommitted\n");

            var pr = GitDiffComparison.load(root, "src/file.txt", true, "main");
            equal("original\n", pr.before(), "PR uses merge base, not advanced target tip");
            equal("feature\nsecond\nthird\n", pr.after(), "PR excludes local edits");
            var local = GitDiffComparison.load(root, "src/file.txt", false, "unused");
            equal(pr.after(), local.before(), "local baseline is HEAD");
            equal("uncommitted\n", local.after(), "local edits are visible");
            var nested = GitDiffComparison.load(root.resolve("src"), "file.txt", true, "main");
            equal(pr.before(), nested.before(), "project root can be a repository subdirectory");

            var added = GitDiffComparison.load(root, "added.txt", true, "main");
            equal("", added.before(), "added file has empty old side");
            equal("added\n", added.after(), "added content");
            var deleted = GitDiffComparison.load(root, "deleted.txt", true, "main");
            equal(true, deleted.deleted(), "deleted files focus old side");
            equal("deleted content\n", deleted.before(), "deleted old content");
            var rename = GitDiffComparison.load(root, "new name.txt", true, "main");
            equal("renamed content\n", rename.before(), "rename preserves old file content");
            equal(rename.before(), rename.after(), "rename compares matched files");
            var empty = GitDiffComparison.load(root, "empty.txt", false, "");
            equal(false, empty.deleted(), "empty file is not missing");
            write(root, "untracked.txt", "new\n");
            equal("new\n", GitDiffComparison.load(root, "untracked.txt", false, "").after(), "untracked file");
            Files.delete(root.resolve("added.txt"));
            equal(true, GitDiffComparison.load(root, "added.txt", false, "").deleted(), "local deletion");
            git(root, "mv", "new name.txt", "staged rename.txt");
            equal("renamed content\n", GitDiffComparison.load(root, "staged rename.txt", false, "").before(), "staged rename");
            String large = "a long line\n".repeat(100_000);
            write(root, "large.txt", large);
            commit(root, "large file");
            equal(large, GitDiffComparison.load(root, "large.txt", false, "").before(), "large Git output does not block");

            fails(root, "src/file.txt", "missing-branch", "invalid base reports failure");
            fails(root, "src/file.txt", "--help", "option-like base cannot inject Git options");
            fails(root, "src/file.txt", "", "empty base reports failure");
            fails(root, "absent.txt", "main", "missing on both sides reports failure");
            fails(root, "../outside.txt", "main", "path traversal rejected");
            git(root, "checkout", "--orphan", "unrelated");
            commit(root, "unrelated history");
            fails(root, "src/file.txt", "main", "unrelated histories report failure");

            range(2, 4, 5, 1, 3);
            range(2, null, 5, 1, 1);
            range(20, 30, 5, 4, 4);
            range(-1, 0, 0, 0, 0);
            range(4, 2, 5, 3, 3);
            System.out.println("Passed " + checks + " diff navigation checks.");
        } finally {
            // Only delete the dedicated directory created above; never follow links.
            if (!root.getFileName().toString().startsWith("amazepr-diff-test-")) throw new IOException("Unexpected test path");
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    path.toFile().setWritable(true);
                    Files.delete(path);
                }
            }
        }
    }

    private static void range(int start, Integer end, int count, int expectedStart, int expectedEnd) {
        equal(Arrays.toString(new int[] {expectedStart, expectedEnd}),
            Arrays.toString(DiffLineRange.clamp(start, end, count)), "line range");
    }

    private static void stepChains(Path root) throws Exception {
        var opener = new ManifestDiffOpener(null);
        var manifest = new ManifestJsonParser().parse(Files.readString(Path.of("../../.change-manifest/manifest.json")));
        equal(false, manifest.steps().isEmpty(), "repository manifest has steps");
        for (WalkthroughStep step : manifest.steps()) {
            if (step.files().isEmpty()) continue;
            var chain = opener.createChain(root, step.files(), 0, false, "");
            equal(step.files().size(), chain.getRequests().size(), "all files included for step " + step.number());
            for (int index = 0; index < step.files().size(); index++) {
                equal(true, chain.getRequests().get(index).getName().startsWith(step.files().get(index).path()),
                    "manifest order preserved");
            }
            var selected = opener.createChain(root, step.files(), step.files().size() - 1, true, "main");
            equal(step.files().size() - 1, selected.getIndex(), "file button opens selected file in full step chain");
        }
        var files = new ArrayList<>(List.of(
            new WorkflowFileReference("same.txt", 2, 3, "First target"),
            new WorkflowFileReference("same.txt", 8, 10, "Second target")));
        var chain = opener.createChain(root, files, 1, false, "");
        files.clear();
        equal(2, chain.getRequests().size(), "distinct targets in same file remain navigable");
        equal(1, chain.getIndex(), "selected target preserved");
        equal("same.txt • Second target", chain.getRequests().get(1).getName(), "target label retained");
        try {
            opener.createChain(root, List.of(), 0, false, "");
            throw new AssertionError("empty step must be rejected");
        } catch (IllegalArgumentException expected) { checks++; }
    }
    private static void fails(Path root, String path, String base, String label) throws Exception {
        try { GitDiffComparison.load(root, path, true, base); }
        catch (IOException expected) { checks++; return; }
        throw new AssertionError(label);
    }

    private static void sidebarNavigation(Path root) throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                var manifest = new ManifestJsonParser().parse(Files.readString(Path.of("../../.change-manifest/manifest.json")));
                for (WalkthroughStep step : manifest.steps()) {
                    if (step.files().isEmpty()) continue;
                    var chain = new ManifestDiffOpener(null).createChain(root, step.files(), 0, false, "");
                    var selected = new java.util.concurrent.atomic.AtomicInteger(-1);
                    int last = step.files().size() - 1;
                    var panel = new StepDiffPanel(chain.getRequests().stream().map(r -> r.getName()).toList(),
                        last, new javax.swing.JPanel(), selected::set);
                    var list = (javax.swing.JList<?>) find(panel, javax.swing.JList.class, null);
                    var previous = (javax.swing.JButton) find(panel, javax.swing.JButton.class, "Previous file");
                    var next = (javax.swing.JButton) find(panel, javax.swing.JButton.class, "Next file");
                    equal(step.files().size(), list.getModel().getSize(), "sidebar exposes all step targets");
                    equal(last, list.getSelectedIndex(), "sidebar starts at the clicked target");
                    equal(-1, selected.get(), "initial selection does not trigger duplicate loads");
                    equal(false, next.isEnabled(), "next disabled at last target");
                    list.setSelectedIndex(0);
                    equal(false, previous.isEnabled(), "previous disabled at first target");
                    if (last > 0) {
                        equal(0, selected.get(), "clicking first target selects its diff producer");
                        next.doClick();
                        equal(1, selected.get(), "next file selects next diff producer");
                        previous.doClick();
                        equal(0, selected.get(), "previous file selects previous diff producer");
                    }
                    selected.set(-1);
                    panel.syncSelection(last);
                    equal(last, list.getSelectedIndex(), "native diff navigation updates sidebar selection");
                    equal(-1, selected.get(), "processor sync does not recurse into loading");
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static java.awt.Component find(java.awt.Container container, Class<?> type, String text) {
        for (java.awt.Component child : container.getComponents()) {
            if (type.isInstance(child) && (text == null || child instanceof javax.swing.JButton button && text.equals(button.getText()))) return child;
            if (child instanceof java.awt.Container nested) {
                var found = find(nested, type, text);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": unexpected result");
        checks++;
    }
    private static void write(Path root, String path, String text) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
    private static void commit(Path root, String message) throws Exception {
        git(root, "add", "-A");
        git(root, "commit", "--allow-empty", "-m", message);
    }
    private static void git(Path root, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IOException(output);
    }
}
