package pl.solutiontabs.tabs;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import javax.swing.SwingConstants;

public final class NativeTabsController {
    private NativeTabsController() {}

    public static void apply(boolean hide) {
        Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            applyOnEdt(hide);
        } else {
            application.invokeLater(() -> applyOnEdt(hide));
        }
    }

    private static void applyOnEdt(boolean hide) {
        UISettings settings = UISettings.getInstance();
        int placement = hide ? UISettings.TABS_NONE : SwingConstants.TOP;
        if (settings.getEditorTabPlacement() != placement) {
            settings.setEditorTabPlacement(placement);
            settings.fireUISettingsChanged();
        }
    }
}
