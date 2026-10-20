package dev.amazepr.plugin;

import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.impl.CacheDiffRequestChainProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import javax.swing.Action;
import javax.swing.JComponent;

/** Owns the native diff processor and a permanent, visible list of all step targets. */
final class StepDiffDialog extends DialogWrapper {
    private final StepProcessor processor;
    private final StepDiffPanel panel;

    StepDiffDialog(Project project, DiffRequestChain chain) {
        super(project, false);
        setModal(false);
        setTitle("aMazePR — Step files (" + chain.getRequests().size() + ")");
        processor = new StepProcessor(project, chain);
        Disposer.register(getDisposable(), processor);
        panel = new StepDiffPanel(chain.getRequests().stream().map(request -> request.getName()).toList(),
            chain.getIndex(), processor.getComponent(), processor::setCurrentRequest);
        processor.addListener(() -> panel.syncSelection(processor.currentIndex()), getDisposable());
        setCancelButtonText("Close");
        init();
        processor.updateRequest();
    }

    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[] {getCancelAction()};
    }

    @Override
    protected String getDimensionServiceKey() {
        return "amazepr.step.diff";
    }

    private static final class StepProcessor extends CacheDiffRequestChainProcessor {
        StepProcessor(Project project, DiffRequestChain chain) {
            super(project, chain);
        }

        int currentIndex() {
            return getRequestChain().getRequests().indexOf(getCurrentRequestProvider());
        }
    }
}
