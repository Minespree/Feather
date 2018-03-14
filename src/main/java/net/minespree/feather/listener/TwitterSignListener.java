package net.minespree.feather.listener;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minespree.babel.Babel;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class TwitterSignListener implements Listener {
    private static final String TWITTER_HANDLE = "@Minespree";

    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = e.getClickedBlock();

        if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) {
            return;
        }

        Sign sign = (Sign) block.getState();
        String[] lines = sign.getLines();

        if (lines[3] == null || !lines[3].equalsIgnoreCase(TWITTER_HANDLE)) {
            return;
        }

        Player player = e.getPlayer();

        String message = Babel.translate("twitter_message").toString(player);
        ComponentBuilder builder = new ComponentBuilder(message).event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://twitter.com/minespree"));
        player.spigot().sendMessage(builder.create());
    }
}
