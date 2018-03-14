package net.minespree.feather.player.rank;

import net.minespree.wizard.util.Chat;
import org.bukkit.ChatColor;

public enum Rank {
    MEMBER("", ChatColor.GRAY, "i"),
    IRON("Iron", ChatColor.WHITE, "h"),
    GOLD("Gold", ChatColor.GOLD, "g"),
    DIAMOND("Diamond", ChatColor.AQUA, "f"),
    VIP("VIP", ChatColor.GOLD, ChatColor.WHITE, "e"),
    YOUTUBE("YouTube", ChatColor.RED, ChatColor.WHITE, "d"),
    HELPER("Helper", ChatColor.LIGHT_PURPLE, "c"),
    MODERATOR("Mod", ChatColor.RED, "b"),
    ADMIN("Admin", ChatColor.DARK_RED, ChatColor.RED, "a");

    private String scoreboardTag;
    private String tag;
    private ChatColor color;
    private ChatColor secondaryColor;

    Rank(String tag, ChatColor color, String scoreboardTag) {
        this(tag, color, color, scoreboardTag);
    }

    Rank(String tag, ChatColor color, ChatColor secondaryColor, String scoreboardTag) {
        this.tag = tag;
        this.color = color;
        this.secondaryColor = secondaryColor;
        this.scoreboardTag = scoreboardTag;
    }

    /**
     * Gets a rank by its name, defaults to {@link Rank#MEMBER} if none match
     */
    public static Rank byName(String name) {
        if (name == null) return Rank.MEMBER;

        for (Rank rank : values()) {
            if (rank.name().equals(name)) {
                return rank;
            }
        }

        return Rank.MEMBER;
    }

    public String getTag() {
        return tag;
    }

    public String getColoredTag() {
        if (this == MEMBER) {
            return MEMBER.getColor().toString();
        }
        return getColor().toString() + Chat.BOLD + getTag() + getColor() + " " + getSecondaryColor();
    }

    public ChatColor getColor() {
        return color;
    }

    public ChatColor getSecondaryColor() {
        return secondaryColor;
    }

    public boolean hasTag() {
        return this != MEMBER;
    }

    public boolean has(Rank rank) {
        return compareTo(rank) >= 0;
    }

    public String getTeamName() {
        return scoreboardTag;
    }
}
