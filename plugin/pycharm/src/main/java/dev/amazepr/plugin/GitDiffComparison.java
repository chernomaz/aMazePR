package dev.amazepr.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Loads immutable diff snapshots without any IDE dependencies. */
record GitDiffComparison(String before, String after, String beforeTitle, String afterTitle, boolean deleted) {
    static GitDiffComparison load(Path projectRoot, String path, boolean pullRequest, String base)
        throws IOException, InterruptedException {
        Path root = projectRoot.toRealPath();
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root) || file.equals(root)) throw new IOException("File must be inside the project: " + path);
        Path gitRoot = Path.of(git(root, "rev-parse", "--show-toplevel").strip()).toRealPath();
        String gitPath = gitRoot.relativize(file).toString().replace('\\', '/');
        String head = revision(root, "HEAD");
        String beforeRevision = head;
        String beforeTitle = "HEAD (" + head.substring(0, 8) + ")";
        if (pullRequest) {
            if (base == null || base.isBlank()) throw new IOException("Enter a PR base branch, for example origin/main.");
            String baseRevision = revision(root, base.strip());
            beforeRevision = git(root, "merge-base", baseRevision, head).strip();
            beforeTitle = "Merge base of " + base.strip() + " (" + beforeRevision.substring(0, 8) + ")";
        }
        // Use the complete diff so rename detection can pair old and new paths.
        List<String> args = new ArrayList<>(List.of("diff", "--name-status", "-z", "-M", beforeRevision));
        if (pullRequest) args.add(head);
        args.add("--");
        String[] changes = git(root, args.toArray(String[]::new)).split("\u0000");
        String previousPath = gitPath;
        for (int i = 0; i + 1 < changes.length;) {
            String kind = changes[i++];
            String oldPath = changes[i++];
            if ((kind.startsWith("R") || kind.startsWith("C")) && i < changes.length) {
                String newPath = changes[i++];
                if (newPath.equals(gitPath)) previousPath = oldPath;
            }
        }
        String before = readRevision(root, beforeRevision, previousPath);
        String after;
        if (pullRequest) {
            after = readRevision(root, head, gitPath);
        } else if (Files.exists(file)) {
            if (!file.toRealPath().startsWith(root) || !Files.isRegularFile(file)) {
                throw new IOException("File must be a regular file inside the project: " + path);
            }
            after = Files.readString(file, StandardCharsets.UTF_8);
        } else {
            after = null;
        }
        if (before == null && after == null) throw new IOException("File does not exist in either comparison side: " + path);
        String afterTitle = pullRequest ? "HEAD (" + head.substring(0, 8) + ")" : "Working tree";
        if (!previousPath.equals(gitPath)) beforeTitle += " • " + previousPath;
        return new GitDiffComparison(before == null ? "" : before, after == null ? "" : after,
            beforeTitle, afterTitle, after == null);
    }

    private static String revision(Path root, String ref) throws IOException, InterruptedException {
        return git(root, "rev-parse", "--verify", "--end-of-options", ref + "^{commit}").strip();
    }

    private static String readRevision(Path root, String revision, String path) throws IOException, InterruptedException {
        String entry = git(root, "ls-tree", "--full-tree", "-z", revision, "--", ":(literal)" + path);
        if (entry.isEmpty()) return null;
        if (!entry.startsWith("100")) throw new IOException("Cannot compare a non-regular Git file: " + path);
        return git(root, "show", revision + ":" + path);
    }

    private static String git(Path root, String... args) throws IOException, InterruptedException {
        Path output = Files.createTempFile("amazepr-git-", ".out");
        Path error = Files.createTempFile("amazepr-git-", ".err");
        Process process = null;
        try {
            List<String> command = new ArrayList<>(List.of("git", "-c", "core.quotepath=false"));
            command.addAll(List.of(args));
            process = new ProcessBuilder(command).directory(root.toFile())
                .redirectOutput(output.toFile()).redirectError(error.toFile()).start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) throw new IOException("Git comparison timed out.");
            if (process.exitValue() != 0) throw new IOException("Git comparison failed: " + Files.readString(error).strip());
            return Files.readString(output, StandardCharsets.UTF_8);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.onExit().join();
            }
            Files.deleteIfExists(output);
            Files.deleteIfExists(error);
        }
    }
}
