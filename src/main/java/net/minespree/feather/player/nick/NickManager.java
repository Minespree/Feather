package net.minespree.feather.player.nick;

import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.ItemUtil;
import net.minespree.feather.util.SingleTimeTextInput;
import net.minespree.wizard.WizardPlugin;
import net.minespree.wizard.util.Chat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NickManager {

    public NickManager() {
        ProtocolManager manager = WizardPlugin.getPlugin().getProtocolManager();
        manager.addPacketListener(new PacketAdapter(FeatherPlugin.get(), PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
                    WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo(event.getPacket());
                    List<PlayerInfoData> newPlayerInfoData = new ArrayList<>();
                    List<PlayerInfoData> oldList = playerInfo.getData();

                    for (PlayerInfoData data : oldList) {
                        if (data == null || data.getProfile() == null || Bukkit.getPlayer(data.getProfile().getUUID()) == null) {
                            newPlayerInfoData.add(data);
                            continue;
                        }

                        WrappedGameProfile profile = data.getProfile();
                        NetworkPlayer np = PlayerManager.getInstance().getPlayer(profile.getUUID());
                        if (np == null || !np.hasNick()) {
                            newPlayerInfoData.add(data);
                            continue;
                        }

                        profile = profile.withName(np.getNick());
                        //profile.getProperties().putAll(data.getProfile().getProperties()); // TODO: Generate skin?
                        PlayerInfoData newData = new PlayerInfoData(profile, data.getPing(), data.getGameMode(), data.getDisplayName());
                        newPlayerInfoData.add(newData);
                    }
                    playerInfo.setData(newPlayerInfoData);
                }
            }
        });

        manager.addPacketListener(new PacketAdapter(FeatherPlugin.get(), PacketType.Play.Server.SCOREBOARD_TEAM) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM) {
                    WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam(event.getPacket());
                    List<String> oldEntries = team.getPlayers();
                    List<String> newEntries = new ArrayList<>();

                    for (String s : oldEntries) {
                        if (s == null || Bukkit.getPlayer(s) == null) {
                            newEntries.add(s);
                            continue;
                        }

                        NetworkPlayer np = PlayerManager.getInstance().getPlayer(Bukkit.getPlayer(s));
                        if (np == null || !np.hasNick()) {
                            newEntries.add(s);
                            continue;
                        }

                        s = np.getNick();
                        newEntries.add(s);
                    }

                    team.setPlayers(newEntries);
                }
            }
        });
    }

    private boolean nick(NetworkPlayer player, String nick, Rank nickedRank) {
        if (player.hasNick()) {
            Babel.translate("already_nicked").sendMessage(player.getPlayer());
            return false;
        }

        FeatherPlugin.get().getStaffChat().publishToStaff(player.getLastKnownUsername() + " has nicked as " + nick + ".");
        player.setNick(nick, nickedRank);

        for (final Player forWhom : player.getPlayer().getWorld().getPlayers()) {
            if (player.getPlayer().equals(forWhom) || !player.getPlayer().getWorld().equals(forWhom.getWorld()) || !forWhom.canSee(player.getPlayer())) {
                forWhom.hidePlayer(player.getPlayer());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        forWhom.showPlayer(player.getPlayer());
                    }
                }.runTaskLater(FeatherPlugin.get(), 2);
            }
        }

        Bukkit.getPluginManager().callEvent(new NickChangeEvent(player, NickChangeEvent.Action.NEW_NICK));
        return true;
    }

    public void resetNick(NetworkPlayer player) {
        FeatherPlugin.get().getStaffChat().publishToStaff(player.getLastKnownUsername() + " is no longer nicked.");
        player.setNick(null, Rank.MEMBER);
        for (final Player forWhom : player.getPlayer().getWorld().getPlayers()) {
            if (player.getPlayer().equals(forWhom) || !player.getPlayer().getWorld().equals(forWhom.getWorld()) || !forWhom.canSee(player.getPlayer())) {
                forWhom.hidePlayer(player.getPlayer());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        forWhom.showPlayer(player.getPlayer());
                    }
                }.runTaskLater(FeatherPlugin.get(), 2);
            }
        }

        Bukkit.getPluginManager().callEvent(new NickChangeEvent(player, NickChangeEvent.Action.RESET_NICK));
    }

    public static class NickSetup {

        private TextComponent RANK_CHOOSER_COMPONENT;
        private ItemStack RANK_CHOOSER_BOOK = ItemUtil.createBook("rank_chosing", "Minespree", Collections.singletonList(RANK_CHOOSER_COMPONENT));
        private TextComponent NAME_CHOOSER_COMPONENT;
        private ItemStack NAME_CHOOSER_BOOK = ItemUtil.createBook("rank_chosing", "Minespree", Collections.singletonList(NAME_CHOOSER_COMPONENT));
        private Rank nickRank;
        private String nick;
        private String skin;
        private Listener localListener;
        private Player player;

        {
            RANK_CHOOSER_COMPONENT = new TextComponent(Chat.BOLD + "Nick Setup\n\nHey there, nice to meet you. I see that you are trying to setup a nickname, " +
                    "to do so I'll need you to select one of the following Ranks:");
            RANK_CHOOSER_COMPONENT.addExtra("\n\n");
            TextComponent memberRank = new TextComponent("➤ " + Chat.GRAY + Chat.BOLD + "Member");
            memberRank.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.GRAY + "Click to set as Member")));
            memberRank.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/localnicksetup rank MEMBER"));
            RANK_CHOOSER_COMPONENT.addExtra(memberRank);
            RANK_CHOOSER_COMPONENT.addExtra("\n");
            TextComponent ironRank = new TextComponent("➤ " + Chat.DARK_GRAY + Chat.BOLD + "Iron");
            ironRank.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.GRAY + "Click to set as " + Chat.WHITE + Chat.BOLD + "Iron")));
            ironRank.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/localnicksetup rank IRON"));
            RANK_CHOOSER_COMPONENT.addExtra(ironRank);
            RANK_CHOOSER_COMPONENT.addExtra("\n");
            TextComponent goldRank = new TextComponent("➤ " + Chat.GOLD + Chat.BOLD + "Gold");
            goldRank.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.GRAY + "Click to set as " + Chat.GOLD + Chat.BOLD + "Gold")));
            goldRank.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/localnicksetup rank GOLD"));
            RANK_CHOOSER_COMPONENT.addExtra(goldRank);
            RANK_CHOOSER_COMPONENT.addExtra("\n");
            TextComponent diamondRank = new TextComponent("➤ " + Chat.AQUA + Chat.BOLD + "Diamond");
            diamondRank.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.GRAY + "Click to set as " + Chat.AQUA + Chat.BOLD + "Diamond")));
            diamondRank.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/localnicksetup rank DIAMOND"));
            RANK_CHOOSER_COMPONENT.addExtra(diamondRank);
        }

        {
            NAME_CHOOSER_COMPONENT = new TextComponent(Chat.BOLD + "Nick Setup\n\nGreat, now you'll need to select the name other players will see you as:");
            NAME_CHOOSER_COMPONENT.addExtra("\n\n");
            TextComponent randomName = new TextComponent("➤ Generate Random Name");
            randomName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.GRAY + "Generate a random name from a pool.")));
            randomName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/localnicksetup name random"));
            NAME_CHOOSER_COMPONENT.addExtra(randomName);
            NAME_CHOOSER_COMPONENT.addExtra("\n");
            TextComponent chooseName = new TextComponent("➤ Choose Name");
            chooseName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Chat.GRAY + "Choose own name.")));
            chooseName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/localnicksetup name choose"));
            NAME_CHOOSER_COMPONENT.addExtra(chooseName);
        }

        private NickSetup(NetworkPlayer networkPlayer) {
            this.player = networkPlayer.getPlayer();

            this.localListener = new Listener() {
                @EventHandler
                public void on(PlayerCommandPreprocessEvent event) {
                    if (event.getMessage().startsWith("/localnicksetup") && event.getPlayer().getUniqueId().equals(networkPlayer.getUuid())) {
                        event.setCancelled(true);

                        String message = event.getMessage().replace("/localnicksetup ", "");
                        String[] rest = message.split(" ");
                        if ("rank".equalsIgnoreCase(rest[0])) {
                            nickRank = Rank.valueOf(rest[1]);
                            player.closeInventory();

                            Bukkit.getScheduler().runTaskLater(FeatherPlugin.get(), () -> ItemUtil.openBook(NAME_CHOOSER_BOOK, player), 1L);
                        } else if ("name".equalsIgnoreCase(rest[0])) {
                            String action = rest[1];
                            if ("random".equalsIgnoreCase(action)) {
                                nick = RandomNameGenerator.generate();

                                player.chat("/localnicksetup end");
                            } else {
                                Babel.translateMulti("type_nick_in_chat").toString(player).forEach(s -> player.sendMessage(Chat.center(s)));

                                new SingleTimeTextInput(player, s -> !s.isEmpty() && StringUtils.isAlphanumeric(s) && s.length() <= 16 && s.length() > 4 && !s.contains(" "),
                                        Chat.RED + "Name cannot be empty, must be longer than 4 characters, cannot be shorter than 16 characters and cannot contain non alpha-numeric characters.", s -> {
                                    nick = s;

                                    player.chat("/localnicksetup end");
                                });
                            }
                        } else if ("end".equalsIgnoreCase(rest[0])) {
                            HandlerList.unregisterAll(this);

                            if (FeatherPlugin.get().getNickManager().nick(networkPlayer, nick, nickRank)) {
                                Babel.translate("now_nicked_as").sendMessage(player, nickRank == Rank.MEMBER ? Chat.GRAY + nick : nickRank.getColoredTag() + nick);
                            }
                        }
                    }
                }
            };

            Bukkit.getPluginManager().registerEvents(localListener, FeatherPlugin.get());

            ItemUtil.openBook(RANK_CHOOSER_BOOK, player);
        }

        public static void start(NetworkPlayer player) {
            new NickSetup(player);
        }

    }

}
