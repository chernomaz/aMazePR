package dev.amazepr.plugin;

final class DiffLineRange {
    private DiffLineRange() {}
    /** Converts an inclusive, one-based manifest range to valid editor line indices. */
    static int[] clamp(int line, Integer endLine, int lineCount) {
        int last = Math.max(0, lineCount - 1);
        int start = Math.min(Math.max(1, line) - 1, last);
        int end = endLine == null ? start : Math.min(Math.max(1, endLine) - 1, last);
        return new int[] {start, Math.max(start, end)};
    }
}
