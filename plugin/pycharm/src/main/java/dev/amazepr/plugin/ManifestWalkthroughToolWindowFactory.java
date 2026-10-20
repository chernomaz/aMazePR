package dev.amazepr.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public final class ManifestWalkthroughToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(
        @NotNull Project project,
        @NotNull ToolWindow toolWindow
    ) {
        ManifestWalkthroughView view = new ManifestWalkthroughView(project);
        Content content = ContentFactory.getInstance().createContent(
            view.component(),
            "",
            false
        );
        toolWindow.getContentManager().addContent(content);
    }
}
