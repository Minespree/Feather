package net.minespree.feather.data.gamedata.kits;

import lombok.Data;
import lombok.NonNull;
import net.minespree.babel.MultiBabelMessage;
import net.minespree.feather.data.gamedata.kits.event.TierSetEvent;
import net.minespree.wizard.util.ItemBuilder;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.BiConsumer;

@Data
public class Tier {

    /**
     *
     */
    @NonNull
    private final MultiBabelMessage description;
    /**
     *
     */
    @NonNull
    private final Map<Integer, ItemBuilder> inventory;
    /**
     *
     */
    @NonNull
    private final Map<Integer, ItemBuilder> armour;
    /**
     *
     */
    @NonNull
    private final Map<String, Document> data;
    /**
     *
     */
    @NonNull
    private final int price, slot;

    public void set(Player player, BiConsumer<String, Document> consumer) {
        inventory.forEach((slot, item) -> player.getInventory().setItem(slot, item.build(player)));
        player.getInventory().setHelmet(armour.getOrDefault(0, new ItemBuilder(Material.AIR)).build(player));
        player.getInventory().setChestplate(armour.getOrDefault(1, new ItemBuilder(Material.AIR)).build(player));
        player.getInventory().setLeggings(armour.getOrDefault(2, new ItemBuilder(Material.AIR)).build(player));
        player.getInventory().setBoots(armour.getOrDefault(3, new ItemBuilder(Material.AIR)).build(player));
        if (consumer != null) {
            data.forEach(consumer);
        }
        Bukkit.getPluginManager().callEvent(new TierSetEvent(player, this));
    }

    public void set(Player player) {
        set(player, null);
    }

}
