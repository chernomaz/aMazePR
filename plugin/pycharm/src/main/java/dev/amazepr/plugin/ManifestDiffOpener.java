package dev.amazepr.plugin;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.chains.DiffRequestProducer;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.tools.simple.SimpleDiffTool;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.DiffUserDataKeysEx;
import com.intellij.diff.util.Side;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ManifestDiffOpener {
    private final Project project;
    ManifestDiffOpener(Project project) { this.project = project; }

    void open(Path root, List<WorkflowFileReference> files, int selectedIndex, boolean pullRequest,
              String base, Consumer<String> status) {
        var chain = createChain(root, files, selectedIndex, pullRequest, base);
        new StepDiffDialog(project, chain).show();
        status.accept("Opened step comparison: " + files.size() + " targets. Select a file in the left sidebar.");
    }

    SimpleDiffRequestChain createChain(Path root, List<WorkflowFileReference> files, int selectedIndex,
                                      boolean pullRequest, String base) {
        if (files.isEmpty()) throw new IllegalArgumentException("A step comparison needs at least one file.");
        List<DiffRequestProducer> producers = new ArrayList<>();
        List<WorkflowFileReference> references = List.copyOf(files);
        for (int index = 0; index < references.size(); index++) {
            WorkflowFileReference reference = references.get(index);
            String title = "aMazePR • " + (index + 1) + "/" + references.size() + " • " + reference.displayLabel();
            producers.add(new DiffRequestProducer() {
                @Override
                public String getName() {
                    String label = reference.displayLabel();
                    return label.equals(reference.path()) ? label : reference.path() + " • " + label;
                }

                @Override
                public DiffRequest process(UserDataHolder context, ProgressIndicator indicator)
                    throws DiffRequestProducerException {
                    indicator.checkCanceled();
                    try {
                        GitDiffComparison comparison = GitDiffComparison.load(root, reference.path(), pullRequest, base);
                        indicator.checkCanceled();
                        var type = FileTypeManager.getInstance().getFileTypeByFileName(reference.path());
                        var factory = DiffContentFactory.getInstance();
                        var request = new SimpleDiffRequest(title,
                            factory.create(project, comparison.before(), type), factory.create(project, comparison.after(), type),
                            comparison.beforeTitle(), comparison.afterTitle());
                        Side side = comparison.deleted() ? Side.LEFT : Side.RIGHT;
                        request.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
                        if (reference.line() != null) {
                            request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(side, Math.max(0, reference.line() - 1)));
                            request.putUserData(DiffUserDataKeys.PREFERRED_FOCUS_SIDE, side);
                            request.putUserData(ManifestDiffRangeExtension.TARGET,
                                new ManifestDiffRangeExtension.Target(side, reference.line(), reference.endLine()));
                            request.putUserData(DiffUserDataKeysEx.FORCE_DIFF_TOOL, SimpleDiffTool.INSTANCE);
                        }
                        return request;
                    } catch (IOException exception) {
                        throw new DiffRequestProducerException(reference.path() + ": " + exception.getMessage(), exception);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new ProcessCanceledException(exception);
                    }
                }
            });
        }
        return SimpleDiffRequestChain.fromProducers(producers, Math.max(0, Math.min(selectedIndex, files.size() - 1)));
    }
}
