package pl.solutiontabs.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import pl.solutiontabs.settings.SolutionTabsSettings;

public final class NativeTabsStartupActivity implements StartupActivity.DumbAware {
    @Override public void runActivity(@NotNull Project project) {
        NativeTabsController.apply(SolutionTabsSettings.get(project).getState().hideNativeEditorTabs);
    }
}
