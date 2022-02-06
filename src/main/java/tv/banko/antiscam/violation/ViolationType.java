package tv.banko.antiscam.violation;

public enum ViolationType {

    NONE(0, 15),
    MEDIUM(16, 35),
    HIGH(36, 55),
    EXTREME(56, 999999);

    private final int minLevel;
    private final int maxLevel;

    ViolationType(int minLevel, int maxLevel) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public static ViolationType getByScore(int score) {
        for (ViolationType type : values()) {

            if (type.getMinLevel() > score) {
                continue;
            }

            if (type.getMaxLevel() < score) {
                continue;
            }

            return type;
        }

        return NONE;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String toString() {
        return "violation-" + super.name().toLowerCase();
    }
}
