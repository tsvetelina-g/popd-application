package app.popdapplication.model.enums;

public enum ArtistRole {
    ACTOR("Actor"),
    DIRECTOR("Director"),
    WRITER("Writer"),
    PRODUCER("Producer"),
    CINEMATOGRAPHER("Cinematographer"),
    EDITOR("Editor"),
    COMPOSER("Composer"),
    COSTUME_DESIGNER("Costume Designer"),
    PRODUCTION_DESIGNER("Production Designer"),
    VISUAL_EFFECTS("Visual Effects"),
    SOUND_DESIGNER("Sound Designer"),
    MAKEUP_ARTIST("Makeup Artist"),
    STUNT_COORDINATOR("Stunt Coordinator"),
    CHOREOGRAPHER("Choreographer"),
    CASTING_DIRECTOR("Casting Director");

    private final String displayName;

    ArtistRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
