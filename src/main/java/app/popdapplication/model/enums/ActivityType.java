package app.popdapplication.model.enums;

public enum ActivityType {
    RATED("Rated"),
    WATCHED("Watched"),
    REVIEWED("Reviewed"),
    ADDED_TO_WATCHLIST("Added to watchlist");

    private final String displayName;

    ActivityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
