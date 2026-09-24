package pl.solutiontabs.settings;

import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.XCollection;
import java.util.ArrayList;
import java.util.List;

public final class SolutionTabsState {
    @OptionTag public GroupMode groupMode = GroupMode.SOLUTION;
    @OptionTag public FileOrder fileOrder = FileOrder.ALPHABETICAL;
    @OptionTag public GroupOrder groupOrder = GroupOrder.ALPHABETICAL;
    @OptionTag public boolean showFullPathInTooltip = true;
    @OptionTag public boolean hideNativeEditorTabs = true;
    @OptionTag public boolean singleClickOpensFile = true;
    @OptionTag public boolean showFileIcons = true;
    @OptionTag public boolean showFileExtensions = true;
    @OptionTag public boolean showCloseButtons = true;
    @XCollection(style = XCollection.Style.v2) public List<TabRule> rules = defaults();

    private static List<TabRule> defaults() {
        List<TabRule> result = new ArrayList<>();
        result.add(new TabRule("C#", "*.cs", "#4E9AEB", "C#"));
        result.add(new TabRule("Projects", "*.csproj;*.fsproj;*.vbproj;*.sln;*.slnx", "#B07CC6", "Solution"));
        result.add(new TabRule("Web", "*.js;*.ts;*.jsx;*.tsx;*.css;*.scss;*.html", "#E8B04A", "Web"));
        result.add(new TabRule("Data", "*.json;*.xml;*.yaml;*.yml", "#64B587", "Data"));
        return result;
    }
}
