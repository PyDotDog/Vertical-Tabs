package pl.solutiontabs.settings;

public enum FileOrder {
    ALPHABETICAL("Alphabetical"),
    RECENTLY_USED("Recently used first"),
    OPENED_NEWEST("Recently opened first"),
    OPENED_OLDEST("Oldest opened first");

    private final String label;
    FileOrder(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
