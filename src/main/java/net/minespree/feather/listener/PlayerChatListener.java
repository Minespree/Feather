package net.minespree.feather.listener;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.data.chat.ChatManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.ArrayUtil;
import net.minespree.feather.util.LoggingUtils;
import net.minespree.feather.util.TimeUtils;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class PlayerChatListener implements Listener {

    private final Map<UUID, MessageRateLimit> rateLimit = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void on(AsyncPlayerChatEvent event) {
        NetworkPlayer player = NetworkPlayer.of(event.getPlayer());

        if (player.isMuted()) {
            event.getRecipients().clear();
            event.setCancelled(true);
            long diff = player.getMutedTill() - System.currentTimeMillis();
            boolean forever = player.getMutedTill() == -1;
            Babel.translate("muted_still").sendMessage(event.getPlayer(), forever ? "Forever" : TimeUtils.formatTime((int) (diff / 1000)));
            return;
        }

        MessageRateLimit mrl = rateLimit.computeIfAbsent(event.getPlayer().getUniqueId(), (k) -> new MessageRateLimit());
        if (mrl.rateLimited()) {
            event.getRecipients().clear();
            event.setCancelled(true);
            Babel.translate("stop_spamming").sendMessage(event.getPlayer());
            return;
        }

        BaseComponent[] hoverTag = new ComponentBuilder("Rank: ").color(net.md_5.bungee.api.ChatColor.GOLD)
                .append(player.hasNick() ? player.getNickedRank() == Rank.MEMBER ? "Member" : player.getNickedRank().getTag() : player.getRank() == Rank.MEMBER ? "Member" : player.getRank().getTag())
                .color(player.hasNick() ? player.getNickedRank().getSecondaryColor().asBungee() : player.getRank().getSecondaryColor().asBungee())
                .append("\nLevel: ")
                .color(net.md_5.bungee.api.ChatColor.GOLD)
                .append(String.valueOf(player.getLevel()))
                .color(net.md_5.bungee.api.ChatColor.GRAY)
                .append("\n\n")
                .append("Click to message.")
                .color(net.md_5.bungee.api.ChatColor.GREEN)
                .create();

        BaseComponent[] name = new ComponentBuilder(event.getPlayer().getDisplayName())
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pm " + player.getPlayer().getName() + " "))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverTag))
                .create();
        BaseComponent[] message = TextComponent.fromLegacyText(" " + ChatColor.DARK_GRAY + "\u00BB " + (player.hasNick() ? ChatColor.GRAY : player.getRank().has(Rank.HELPER) ? ChatColor.WHITE : ChatColor.GRAY) + event.getMessage());
        BaseComponent[] concat = ArrayUtil.concat(BaseComponent[].class, name, message);
        event.getRecipients().stream().filter(p -> !NetworkPlayer.of(p).getIgnoredPlayers().contains(event.getPlayer().getUniqueId())).map(Player::spigot).forEach(spigot -> spigot.sendMessage(concat));
        event.setCancelled(true);

        event.setFormat("%s" + ChatColor.DARK_GRAY + " " + Chat.SMALL_ARROWS_RIGHT + " " + (player.getRank().has(Rank.HELPER) ? ChatColor.WHITE : ChatColor.GRAY) + "%s");
        LoggingUtils.log(Level.INFO, String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage()));
        String text = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
        ChatManager.ChatData chatData = new ChatManager.ChatData(event.getPlayer(),
                text,
                System.currentTimeMillis(),
                Bukkit.getServerName(),
                event.getMessage(),
                ChatManager.ChatType.PUBLIC,
                null
        );

        ChatManager manager = FeatherPlugin.get().getChatManager();
        if (manager != null) {
            manager.insertMessage(chatData);
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        rateLimit.remove(event.getPlayer().getUniqueId());
    }

    private class MessageRateLimit {

        private AtomicInteger messagesSent = new AtomicInteger();
        private volatile long expiresAt;

        public MessageRateLimit() {
            expiresAt = System.currentTimeMillis() + 2000;
        }

        public boolean rateLimited() {
            if (System.currentTimeMillis() > expiresAt) {
                expiresAt = System.currentTimeMillis() + 2000;
                messagesSent.set(0);
                return false;
            }
            return messagesSent.incrementAndGet() > 3;
        }
    }
}
