package pl.solutiontabs.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.FileEditorManager;
import pl.solutiontabs.tabs.NativeTabsController;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
@State(name = "SolutionTabs", storages = @Storage("solutionTabs.xml"))
public final class SolutionTabsSettings implements PersistentStateComponent<SolutionTabsState> {
    public interface Listener { void settingsChanged(); }
    public static final Topic<Listener> TOPIC = Topic.create("Vertical Tabs settings", Listener.class);

    private final Project project;
    private SolutionTabsState state = new SolutionTabsState();

    public SolutionTabsSettings(Project project) { this.project = project; }
    public static SolutionTabsSettings get(Project project) { return project.getService(SolutionTabsSettings.class); }
    @Override public @NotNull SolutionTabsState getState() { return state; }
    @Override public void loadState(@NotNull SolutionTabsState state) { this.state = state; }
    public void replace(SolutionTabsState state) {
        this.state = state;
        for (var file : FileEditorManager.getInstance(project).getOpenFiles()) {
            FileEditorManager.getInstance(project).updateFilePresentation(file);
        }
        NativeTabsController.apply(state.hideNativeEditorTabs);
        project.getMessageBus().syncPublisher(TOPIC).settingsChanged();
    }
}
