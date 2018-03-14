package net.minespree.feather.data.gamedata.kits;

import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.gamedata.kits.event.TierSetEvent;
import net.minespree.feather.player.implementations.KittedPlayer;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class KitExtension implements Listener {

    private Map<UUID, String> kitUsing = new HashMap<>(); // Fixes bug where changing mid game changes what they're using
    private Map<UUID, Tier> tiers = new HashMap<>();

    private GameRegistry.Type game;
    private String kitId;

    public KitExtension(GameRegistry.Type game, String kitId) {
        this.game = game;
        this.kitId = kitId;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, FeatherPlugin.get());
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    public boolean isUsing(Player player) {
        KitManager manager = KitManager.getInstance();
        if (manager.isLoaded()) {
            if (kitUsing.containsKey(player.getUniqueId())) {
                if (kitUsing.get(player.getUniqueId()).equals(kitId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Document getDocument(Player player, String key) {
        return tiers.get(player.getUniqueId()).getData().getOrDefault(key, new Document());
    }

    public abstract void setKit(Player player, Tier tier);

    @EventHandler
    public void onTierSet(TierSetEvent event) {
        tiers.put(event.getPlayer().getUniqueId(), event.getTier());
        KittedPlayer kittedPlayer = KitManager.getInstance().getPlayer(event.getPlayer());
        PlayerKit kit = kittedPlayer.getDefaultKit(game);
        kitUsing.put(event.getPlayer().getUniqueId(), kit.getKit().getKitId());

        if (isUsing(event.getPlayer())) {
            setKit(event.getPlayer(), event.getTier());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tiers.remove(event.getPlayer().getUniqueId());
        kitUsing.remove(event.getPlayer().getUniqueId());
    }

}
