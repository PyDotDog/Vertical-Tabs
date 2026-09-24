package pl.solutiontabs.tabs;

import com.intellij.ide.ui.UISettings;
import javax.swing.SwingConstants;

public final class NativeTabsController {
    private NativeTabsController() {}

    public static void apply(boolean hide) {
        UISettings settings = UISettings.getInstance();
        int placement = hide ? UISettings.TABS_NONE : SwingConstants.TOP;
        if (settings.getEditorTabPlacement() != placement) {
            settings.setEditorTabPlacement(placement);
            settings.fireUISettingsChanged();
        }
    }
}
