package dev.amazepr.plugin;

import java.util.List;

record WalkthroughStep(
    int number,
    String title,
    String description,
    List<WorkflowFileReference> files
) {
}
