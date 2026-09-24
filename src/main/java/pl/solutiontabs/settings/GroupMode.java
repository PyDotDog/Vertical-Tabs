package pl.solutiontabs.settings;

public enum GroupMode {
    SOLUTION("IDE modules / .NET projects"),
    EXTENSION("File extension"),
    DIRECTORY("Parent directory"),
    CUSTOM_RULES("Custom rules");

    private final String label;
    GroupMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
