package net.minespree.feather.experience;

import com.google.common.base.Preconditions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.wizard.util.Chat;
import org.bukkit.entity.Player;

public class Experience {
    private static final BabelMessage EXP_AWARDED_MSG = Babel.translate("gain-experience");
    private static final BabelMessage LEVEL_UP_MSG = Babel.translate("level-up");
    private static final BabelMessage EXPERIENCE_NAME_MSG = Babel.translate("experience");
    private static final BabelMessage LEVEL_UP_INFO_MSG = Babel.translate("level-up-info");

    public static void sendMessage(NetworkPlayer player, long amount, String reason) {
        Player bukkit = player.getPlayer();
        Preconditions.checkNotNull(bukkit);

        ComponentBuilder builder = new ComponentBuilder(EXP_AWARDED_MSG.toString(bukkit, amount)).color(ChatColor.BLUE);

        ComponentBuilder hoverMessage = new ComponentBuilder(Chat.PLUS + amount + " " + EXPERIENCE_NAME_MSG.toString(bukkit));
        hoverMessage.color(ChatColor.GREEN).append("\n\n").append("For: ").color(ChatColor.BLUE);
        hoverMessage.append(reason).color(ChatColor.GRAY);

        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage.create()));

        player.sendMessage(builder.create());
    }

    public static void sendLevelUp(NetworkPlayer player, int level) {
        Player bukkit = player.getPlayer();
        Preconditions.checkNotNull(bukkit);

        bukkit.sendMessage(Chat.GREEN + Chat.SEPARATOR);
        bukkit.sendMessage(" ");
        bukkit.sendMessage(Chat.center(Chat.GOLD + LEVEL_UP_MSG.toString(bukkit)));
        bukkit.sendMessage(" ");
        bukkit.sendMessage(Chat.center(Chat.YELLOW + LEVEL_UP_INFO_MSG.toString(bukkit, level)));

        // TODO Add rewards per level

        bukkit.sendMessage(" ");
        bukkit.sendMessage(Chat.GREEN + Chat.SEPARATOR);
    }
}
