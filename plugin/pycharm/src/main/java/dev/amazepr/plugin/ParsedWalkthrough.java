package dev.amazepr.plugin;

import java.util.List;

record ParsedWalkthrough(
    String title,
    String intent,
    List<String> diagnostics,
    List<WalkthroughStep> steps
) {
}
