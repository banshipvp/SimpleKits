package local.simplekits;

public enum RankTier {
    DEFAULT("default", "§7Default", 0),
    SCOUT("scout", "§aScout", 1),
    MILITANT("militant", "§eMilitant", 2),
    TACTICIAN("tactician", "§6Tactician", 3),
    WARLORD("warlord", "§5Warlord", 4),
    SOVEREIGN("sovereign", "§cSovereign", 5);

    private final String id;
    private final String displayName;
    private final int level;

    RankTier(String id, String displayName, int level) {
        this.id = id;
        this.displayName = displayName;
        this.level = level;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public static RankTier fromGroup(String group) {
        if (group == null) return DEFAULT;
        for (RankTier tier : values()) {
            if (tier.id.equalsIgnoreCase(group)) return tier;
        }
        return DEFAULT;
    }
}
