package app.popdapplication.model.enums;

public enum GenreType {

    ACTION("Action"),
    ADVENTURE("Adventure"),
    ANIMATION("Animation"),
    COMEDY("Comedy"),
    CRIME("Crime"),
    DOCUMENTARY("Documentary"),
    DRAMA("Drama"),
    FAMILY("Family"),
    FANTASY("Fantasy"),
    HISTORY("History"),
    HORROR("Horror"),
    MUSIC("Music"),
    MYSTERY("Mystery"),
    ROMANCE("Romance"),
    SCI_FI("Sci‑Fi"),
    THRILLER("Thriller"),
    WAR("War"),
    WESTERN("Western"),
    BIOGRAPHY("Biography"),
    SPORT("Sport");

    private final String displayName;

    GenreType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
