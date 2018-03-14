package net.minespree.feather.settings;

import lombok.Getter;
import net.minespree.feather.repository.types.EnumType;
import org.bukkit.ChatColor;

@Getter
public enum MessageVisibility {
    ANYONE("setting_message1", "setting_message1_lore", ChatColor.GREEN, 10),
    SOME("setting_message2", "setting_message2_lore", ChatColor.GOLD, 14),
    NONE("setting_message3", "setting_message3_lore", ChatColor.GRAY, 8);

    public static final EnumType<MessageVisibility> TYPE = new EnumType<>("msgvis", MessageVisibility.class);

    private String babel;
    private String description;
    private ChatColor color;
    private short data;

    MessageVisibility(String babel, String description, ChatColor color, int data) {
        this.babel = babel;
        this.description = description;
        this.color = color;
        this.data = (short) data;
    }
}
