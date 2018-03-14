package net.minespree.feather.util;

import net.minespree.feather.FeatherPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class SingleTimeTextInput {

    public SingleTimeTextInput(Player player, Predicate<String> filter, String filterError, Consumer<String> acceptor) {
        Listener listener = new Listener() {
            @EventHandler
            public void on(AsyncPlayerChatEvent event) {
                if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    event.setCancelled(true);

                    if (filter.test(event.getMessage())) {
                        acceptor.accept(event.getMessage());

                        HandlerList.unregisterAll(this);
                    } else {
                        event.getPlayer().sendMessage(filterError);
                    }
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, FeatherPlugin.get());
    }

}
