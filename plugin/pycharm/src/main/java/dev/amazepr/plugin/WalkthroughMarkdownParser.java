package dev.amazepr.plugin;

import java.util.ArrayList;
import java.util.List;

final class WalkthroughMarkdownParser {
    ParsedWalkthrough parse(String markdown) {
        String[] lines = markdown.split("\\R");
        String title = "PR Walkthrough";
        String intent = "";
        List<WalkthroughStep> steps = new ArrayList<>();

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.startsWith("# ")) {
                title = line.substring(2).trim();
                continue;
            }
            if ("## Intent".equals(line)) {
                intent = readParagraph(lines, index + 1);
                continue;
            }
            if (line.startsWith("### ")) {
                StepParseResult result = parseStep(lines, index);
                if (result.step() != null) {
                    steps.add(result.step());
                    index = result.nextIndex();
                }
            }
        }

        return new ParsedWalkthrough(title, intent, List.of(), steps);
    }

    private String readParagraph(String[] lines, int start) {
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty()) {
                if (!builder.isEmpty()) {
                    break;
                }
                continue;
            }
            if (line.startsWith("#")) {
                break;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private StepParseResult parseStep(String[] lines, int start) {
        String rawTitle = lines[start].trim().substring(4).trim();
        int dot = rawTitle.indexOf('.');
        int number = start + 1;
        String title = rawTitle;
        if (dot > 0) {
            try {
                number = Integer.parseInt(rawTitle.substring(0, dot).trim());
                title = rawTitle.substring(dot + 1).trim();
            } catch (NumberFormatException ignored) {
                title = rawTitle;
            }
        }

        StringBuilder description = new StringBuilder();
        List<WorkflowFileReference> files = new ArrayList<>();
        boolean inFiles = false;
        int index = start + 1;

        for (; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.startsWith("### ") || line.startsWith("## ")) {
                index--;
                break;
            }
            if (line.isEmpty()) {
                continue;
            }
            if ("Files:".equals(line)) {
                inFiles = true;
                continue;
            }
            if (inFiles && line.startsWith("- `") && line.endsWith("`")) {
                files.add(new WorkflowFileReference(line.substring(3, line.length() - 1), null, null, null));
                continue;
            }
            if (!inFiles) {
                if (!description.isEmpty()) {
                    description.append(' ');
                }
                description.append(line);
            }
        }

        return new StepParseResult(
            new WalkthroughStep(number, title, description.toString(), files),
            index
        );
    }

    private record StepParseResult(WalkthroughStep step, int nextIndex) {
    }
}
