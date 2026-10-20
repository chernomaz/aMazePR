package dev.amazepr.plugin;

import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffExtension;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.util.base.DiffViewerListener;
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer;
import com.intellij.diff.util.Side;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.util.Key;

/** Applies manifest ranges only to aMazePR requests, after diff folding and layout. */
public final class ManifestDiffRangeExtension extends DiffExtension {
    static final Key<Target> TARGET = Key.create("amazepr.diff.target");
    record Target(Side side, int line, Integer endLine) {}

    @Override
    public void onViewerCreated(FrameDiffTool.DiffViewer viewer, DiffContext context, DiffRequest request) {
        Target target = request.getUserData(TARGET);
        if (target == null || !(viewer instanceof TwosideTextDiffViewer textViewer)) return;
        textViewer.addListener(new DiffViewerListener() {
            private boolean focused;
            @Override
            protected void onAfterRediff() {
                if (focused) return;
                focused = true;
                var editor = textViewer.getEditor(target.side());
                var document = editor.getDocument();
                int[] lines = DiffLineRange.clamp(target.line(), target.endLine(), document.getLineCount());
                var folding = editor.getFoldingModel();
                int start = document.getLineStartOffset(lines[0]);
                int end = document.getLineEndOffset(lines[1]);
                folding.runBatchFoldingOperation(() -> {
                    for (var region : folding.getAllFoldRegions()) {
                        if (region.getStartOffset() <= end && region.getEndOffset() >= start) region.setExpanded(true);
                    }
                });
                editor.getCaretModel().moveToOffset(start);
                editor.getSelectionModel().setSelection(start, end);
                editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            }
        });
    }
}
