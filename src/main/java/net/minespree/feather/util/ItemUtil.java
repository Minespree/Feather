package net.minespree.feather.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutCustomPayload;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaBook;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class ItemUtil {

    private ItemUtil() {
        throw new UnsupportedOperationException("ItemUtil cannot be instantiated!");
    }

    public static Map<String, Object> toMap(ItemStack stack) {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        result.put("material", stack.getType().name());
        result.put("amount", stack.getAmount());
        result.put("durability", stack.getDurability());
        result.put("data", stack.getData().getData());

        Map<String, Integer> enchantments = ImmutableMap.copyOf(stack.getEnchantments().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue)));
        if (!enchantments.isEmpty()) {
            result.put("enchantments", ImmutableMap.copyOf(enchantments));
        }

        Map<String, Object> metadata = Maps.newHashMap();
        if (stack.getItemMeta().hasDisplayName()) {
            metadata.put("display_name", stack.getItemMeta().getDisplayName());
        }

        if (stack.getItemMeta().hasLore()) {
            metadata.put("lore", stack.getItemMeta().getLore());
        }

        if (!stack.getItemMeta().getItemFlags().isEmpty()) {
            metadata.put("flags", stack.getItemMeta().getItemFlags().stream().map(ItemFlag::name).collect(Collectors.toSet()));
        }

        if (!metadata.isEmpty()) {
            result.put("metadata", ImmutableMap.copyOf(metadata));
        }

        return result.build();
    }

    public static ItemStack fromDBObject(DBObject object) {
        Preconditions.checkArgument(object instanceof BasicDBObject, "object is not a BasicDBObject");
        return fromMap(BasicDBObject.class.cast(object));
    }

    @SuppressWarnings("unchecked")
    public static ItemStack fromMap(Map<String, Object> map) {
        Preconditions.checkNotNull(map, "map");
        Material material;
        try {
            material = Material.valueOf((String) map.get("material"));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Failed to find Material");
        }
        ItemStack stack = new ItemStack(material);

        Number quantity = (Number) map.get("amount");
        if (quantity != null) {
            stack.setAmount(quantity.intValue());
        }

        Number durability = (Number) map.get("durability");
        if (durability != null) {
            stack.setDurability(durability.shortValue());
        }

        Number data = (Number) map.get("data");
        if (data != null) {
            MaterialData materialData = stack.getData();
            materialData.setData(data.byteValue());
            stack.setData(materialData);
        }

        Map<String, Object> enchantments = (Map<String, Object>) map.get("enchantments");
        if (enchantments != null) {
            for (Map.Entry<String, Object> entry : enchantments.entrySet()) {
                Enchantment enchantment = Enchantment.getByName(entry.getKey());
                if (enchantment == null) {
                    LoggingUtils.log(Level.WARNING, "Invalid Enchantment in ItemStack descriptor");
                    continue;
                }
                Number level = (Number) entry.getValue();
                stack.addUnsafeEnchantment(enchantment, level.intValue());
            }
        }

        Map<String, Object> metadata = (Map<String, Object>) map.get("metadata");
        if (metadata != null) {
            ItemMeta meta = stack.getItemMeta();

            String displayName = (String) metadata.get("display_name");
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }

            List<String> lore = (List<String>) metadata.get("lore");
            if (lore != null) {
                meta.setLore(lore);
            }

            List<String> flags = (List<String>) metadata.get("flags");
            if (flags != null) {
                for (String flag : flags) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(flag));
                    } catch (IllegalArgumentException ex) {
                        LoggingUtils.log(Level.WARNING, "Invalid ItemFlag in ItemStack descriptor");
                    }
                }
            }

            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static void openBook(ItemStack book, Player p) {
        int slot = p.getInventory().getHeldItemSlot();
        ItemStack old = p.getInventory().getItem(slot);
        p.getInventory().setItem(slot, book);

        try {
            ByteBuf buf = Unpooled.buffer(256);
            buf.setByte(0, (byte) 0);
            buf.writerIndex(1);

            PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(buf));
            (((CraftPlayer) p)).getHandle().playerConnection.sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        p.getInventory().setItem(slot, old);
    }

    public static ItemStack createBook(String title, String author, List<BaseComponent> pages) {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta meta = (BookMeta) stack.getItemMeta();

        List<IChatBaseComponent> bookPages;

        try {
            bookPages = (List<IChatBaseComponent>) CraftMetaBook.class.getDeclaredField("pages").get(meta);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return stack;
        }

        for (BaseComponent component : pages) {
            bookPages.add(IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(component)));
        }
        meta.setTitle(title);
        meta.setAuthor(author);
        stack.setItemMeta(meta);
        return stack;
    }

}