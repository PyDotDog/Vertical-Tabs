package pl.solutiontabs.settings;

import com.intellij.util.xmlb.annotations.Attribute;

public final class TabRule {
    @Attribute public String name = "C# files";
    @Attribute public String pattern = "*.cs";
    @Attribute public String color = "#4E9AEB";
    @Attribute public String group = "C#";

    public TabRule() {}
    public TabRule(String name, String pattern, String color, String group) {
        this.name = name; this.pattern = pattern; this.color = color; this.group = group;
    }

    public TabRule copy() { return new TabRule(name, pattern, color, group); }
}
