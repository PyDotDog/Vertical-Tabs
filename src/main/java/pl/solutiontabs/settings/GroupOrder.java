package pl.solutiontabs.settings;

public enum GroupOrder {
    ALPHABETICAL("Alphabetical"),
    RECENTLY_USED("Recently used first"),
    RULE_PRIORITY("Custom rule priority");

    private final String label;
    GroupOrder(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
