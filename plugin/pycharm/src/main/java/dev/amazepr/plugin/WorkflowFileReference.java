package dev.amazepr.plugin;

record WorkflowFileReference(String path, Integer line, Integer endLine, String label) {
    String displayLabel() {
        if (label != null && !label.isBlank()) {
            return label;
        }
        if (line != null && endLine != null && !line.equals(endLine)) {
            return path + ":" + line + "-" + endLine;
        }
        if (line != null) {
            return path + ":" + line;
        }
        return path;
    }
}
