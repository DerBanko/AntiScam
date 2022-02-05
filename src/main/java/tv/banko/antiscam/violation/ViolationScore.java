package tv.banko.antiscam.violation;

public enum ViolationScore {

    // url required
    EVERYONE_PING(5, "@everyone"),

    CONTAINS_FREE(5, "free"),

    CONTAINS_NITRO(15, "nitro"),
    CONTAINS_STEAM(15, "steam"),
    CONTAINS_AIRDROP(15, "airdrop"),
    CONTAINS_DISCORD(15, "discord"),
    CONTAINS_GIFT(15, "gift"),

    CONTAINS_DEFAULT_PHRASES(15, "who is first");

    private final int score;
    private final String[] phrases;

    ViolationScore(int score, String... phrases) {
        this.phrases = phrases;
        this.score = score;
    }

    ViolationScore(int score) {
        this.phrases = null;
        this.score = score;
    }

    public String[] getPhrases() {
        return phrases;
    }

    public int getScore() {
        return score;
    }
}
