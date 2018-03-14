package net.minespree.feather.settings;

import lombok.Getter;
import net.minespree.feather.repository.types.EnumType;
import org.bukkit.ChatColor;

@Getter
public enum Visibility {
    ALL("visibility_all", ChatColor.GREEN, 10),
    FRIENDS("visibility_friends", ChatColor.GOLD, 14),
    NONE("visibility_none", ChatColor.GRAY, 8);

    public static final EnumType<Visibility> TYPE = new EnumType<>("visibility", Visibility.class);

    private String babel;
    private ChatColor color;
    private short data;

    Visibility(String babel, ChatColor color, int data) {
        this.babel = babel;
        this.color = color;
        this.data = (short) data;
    }
}
