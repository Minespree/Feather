package net.minespree.feather.command;

import com.google.common.util.concurrent.FutureCallback;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.db.redis.JedisPublisher;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.punishments.Punishment;
import net.minespree.feather.player.punishments.PunishmentType;
import net.minespree.feather.player.punishments.TieredPunishment;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.Scheduler;
import net.minespree.feather.util.TimeUtils;
import net.minespree.feather.util.UUIDNameKeypair;
import net.minespree.wizard.gui.InventoryGUI;
import net.minespree.wizard.util.ItemBuilder;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PunishmentCommands {

    private static int[] mapped_menu_items = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 35, 38, 39, 40, 41, 42, 43, 44, 45};

    public static void hookRedisListener() {
        RedisManager.getInstance().registerListener("punishments", (channel, object) -> {
            PunishmentType type = PunishmentType.valueOf((String) object.get("punishmentType"));
            UUID target = UUID.fromString((String) object.get("target"));
            String reason = (String) object.get("reason");
            String nonParsedTill = (String) object.get("till");
            long till;
            if (nonParsedTill == null) {
                till = System.currentTimeMillis();
            } else {
                till = Long.valueOf(nonParsedTill);
            }

            Player player;
            if ((player = Bukkit.getPlayer(target)) != null) {
                switch (type) {
                    case BAN:
                    case H_BAN:
                    case H_TEMP_BAN:
                    case TEMP_BAN:
                        runSync(() -> player.kickPlayer(ChatColor.RED + "You have been " + ((type == PunishmentType.BAN || type == PunishmentType.H_BAN) ? "permanently banned" : "temporarily banned") + " from the server." + "\n" + ChatColor.YELLOW + "Appeal at https://minespree.net/appeal/"));
                        break;
                    case E_KICK:
                    case M_KICK:
                    case KICK:
                        runSync(() -> player.kickPlayer(ChatColor.RED + "You have been kicked for " + ChatColor.RESET + reason));
                        break;
                    case MUTE:
                        NetworkPlayer np = NetworkPlayer.of(player);
                        np.setMuted(till);
                        long diff = till - System.currentTimeMillis();
                        Babel.translate("muted_player").sendMessage(player, TimeUtils.formatTime((int) (diff / 1000)));
                        break;
                }
            }
        });
    }

    private static void runSync(Runnable run) {
        FeatherPlugin.get().getServer().getScheduler().runTask(FeatherPlugin.get(), run);
    }

    @Command(names = {"punish"}, requiredRank = Rank.HELPER, async = true)
    public static void punish(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Tiered") String punishment, @Param(name = "Reason", wildcard = true) String reason) {
        UUID source = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        TieredPunishment tiered;
        try {
            tiered = TieredPunishment.valueOf(punishment);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Tiered Possibilities: " + StringUtils.join(TieredPunishment.values(), ", "));
            return;
        }

        MongoCollection<Document> collection = MongoManager.getInstance().getCollection("punishments");

        switch (tiered) {
            case SWEARING:
            case SPAMMING:
            case ADVERTISING:
            case MALICIOUS_CHAT:
            case BULLYING:
                AtomicInteger muteScore = new AtomicInteger(0);
                Scheduler.runAsync(() -> collection.find(Filters.and(Filters.eq("target", target.getUuid().toString()), Filters.or(Filters.eq("type", "MUTE"), Filters.eq("type", "M_KICK")))), new FutureCallback<FindIterable<Document>>() {
                    @Override
                    public void onSuccess(@Nullable FindIterable<Document> documents) {
                        if (documents == null) {
                            muteScore.set(0);
                        } else {
                            for (Document doc : documents) {
                                int score = doc.containsKey("score") ? doc.getInteger("score") : 1;
                                muteScore.addAndGet(score);
                            }

                            int next = muteScore.get();
                            if (tiered == TieredPunishment.MALICIOUS_CHAT) {
                                next += 4; // Insta perma mute.
                            } else if (tiered == TieredPunishment.BULLYING) {
                                next += 2;
                            } else {
                                next += 1;
                            }

                            Punishment p;
                            if (next == 1) {
                                p = new Punishment(PunishmentType.M_KICK, source, target.getUuid(), reason + " MTrack [" + next + "]", System.currentTimeMillis(), -1);
                            } else if (next == 2) {
                                p = new Punishment(PunishmentType.MUTE, source, target.getUuid(), reason + " MTrack [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
                            } else if (next == 3) {
                                p = new Punishment(PunishmentType.MUTE, source, target.getUuid(), reason + " MTrack [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));
                            } else if (next == 4) {
                                p = new Punishment(PunishmentType.MUTE, source, target.getUuid(), reason + " MTrack [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(31));
                            } else {
                                p = new Punishment(PunishmentType.MUTE, source, target.getUuid(), reason + " MTrack [" + next + "]", System.currentTimeMillis(), -1);
                            }

                            p.save();
                            publishStaffChat(p, sender, target.getName());
                            JedisPublisher.create("punishments").set("punishmentType", p.getType()).set("target", target.getUuid().toString()).set("reason", reason).set("till", p.getUntil()).publish();

                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                Babel.translate("punished_player").sendMessage(player, target.getName());

                                noteSuggestion(p, player);
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Punished " + target.getName());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
                break;
            case HACKING:
                AtomicInteger hackingScore = new AtomicInteger(0);
                Scheduler.runAsync(() -> collection.find(Filters.and(Filters.eq("target", target.getUuid().toString()), Filters.or(Filters.eq("type", PunishmentType.H_BAN.name()),
                        Filters.eq("type", PunishmentType.H_TEMP_BAN.name())))), new FutureCallback<FindIterable<Document>>() {
                    @Override
                    public void onSuccess(@Nullable FindIterable<Document> documents) {
                        if (documents == null) {
                            hackingScore.set(0);
                        } else {
                            for (Document doc : documents) {
                                int score = doc.containsKey("score") ? doc.getInteger("score") : 1;
                                hackingScore.addAndGet(score);
                            }

                            int next = hackingScore.addAndGet(1);
                            Punishment p;
                            if (next == 1) {
                                p = new Punishment(PunishmentType.H_TEMP_BAN, source, target.getUuid(), reason + " H [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(14));
                            } else if (next == 2) {
                                p = new Punishment(PunishmentType.H_TEMP_BAN, source, target.getUuid(), reason + " H [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(31));
                            } else {
                                p = new Punishment(PunishmentType.H_BAN, source, target.getUuid(), reason + " H [" + next + "]", System.currentTimeMillis(), -1);
                            }
                            p.save();
                            publishStaffChat(p, sender, target.getName());
                            JedisPublisher.create("punishments").set("punishmentType", p.getType()).set("target", target.getUuid().toString()).set("reason", reason).set("till", p.getUntil()).publish();

                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                Babel.translate("punished_player").sendMessage(player, target.getName());

                                noteSuggestion(p, player);
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Punished " + target.getName());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
                break;
            case EVASION:
                Scheduler.runAsync(() -> collection.find(Filters.and(Filters.eq("target", target.getUuid().toString()), Filters.or(Filters.eq("type", PunishmentType.BAN.name()),
                        Filters.eq("type", PunishmentType.TEMP_BAN.name()), Filters.eq("type", PunishmentType.H_TEMP_BAN.name()), Filters.eq("type", PunishmentType.H_BAN.name()))))
                        .sort(new Document("timestamp", -1)).first(), new FutureCallback<Document>() {
                    @Override
                    public void onSuccess(@Nullable Document documents) {
                        if (documents == null) {
                            sender.sendMessage(ChatColor.RED + "No ongoing punishments found, cannot ban for evasion.");
                        } else {
                            long time = documents.getLong("until");
                            PunishmentType type = PunishmentType.valueOf(documents.getString("type"));
                            Punishment p = new Punishment(type, source, target.getUuid(), "Ban Evasion", System.currentTimeMillis(), time);

                            p.save();
                            publishStaffChat(p, sender, target.getName());
                            JedisPublisher.create("punishments").set("punishmentType", p.getType()).set("target", target.getUuid().toString()).set("reason", reason).set("till", p.getUntil()).publish();

                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                Babel.translate("punished_player").sendMessage(player, target.getName());

                                noteSuggestion(p, player);
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Punished " + target.getName());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
                break;
            case SKIN:
            case NAME:
                Punishment p = new Punishment(PunishmentType.BAN, source, target.getUuid(), reason, System.currentTimeMillis(), -1);
                p.save();

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Babel.translate("punished_player").sendMessage(player, target.getName());

                    noteSuggestion(p, player);
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Punished " + target.getName());
                }
                break;
            case EXPLOITS:
                AtomicInteger exploitScore = new AtomicInteger(0);
                Scheduler.runAsync(() -> collection.find(Filters.and(Filters.eq("target", target.getUuid().toString()), Filters.and(
                        Filters.eq("type", PunishmentType.E_KICK.name()),
                        Filters.eq("type", PunishmentType.E_TEMP_BAN.name())
                ))), new FutureCallback<FindIterable<Document>>() {
                    @Override
                    public void onSuccess(@Nullable FindIterable<Document> documents) {
                        if (documents == null) {
                            exploitScore.set(0);
                        } else {
                            for (Document doc : documents) {
                                int score = doc.containsKey("score") ? doc.getInteger("score") : 1;
                                exploitScore.addAndGet(score);
                            }

                            int next = exploitScore.addAndGet(1);
                            Punishment p;
                            if (next == 1) {
                                p = new Punishment(PunishmentType.KICK, source, target.getUuid(), reason + " ETrack [" + next + "]", System.currentTimeMillis(), -1);
                            } else if (next == 2) {
                                p = new Punishment(PunishmentType.E_TEMP_BAN, source, target.getUuid(), reason + " ETrack [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
                            } else if (next == 3) {
                                p = new Punishment(PunishmentType.E_TEMP_BAN, source, target.getUuid(), reason + " ETrack [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3));
                            } else {
                                p = new Punishment(PunishmentType.E_TEMP_BAN, source, target.getUuid(), reason + " ETrack [" + next + "]", System.currentTimeMillis(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));
                            }

                            p.save();
                            publishStaffChat(p, sender, target.getName());
                            JedisPublisher.create("punishments").set("punishmentType", p.getType()).set("target", target.getUuid().toString()).set("reason", reason).set("till", p.getUntil()).publish();

                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                Babel.translate("punished_player").sendMessage(player, target.getName());

                                noteSuggestion(p, player);
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Punished " + target.getName());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
                break;
            default:
                // wat
        }
    }

    @Command(names = {"ban"}, requiredRank = Rank.ADMIN, hideFromHelp = true)
    public static void ban(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Reason", wildcard = true) String reason) {
        UUID source = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        Punishment punishment = new Punishment(PunishmentType.BAN, source, target.getUuid(), reason, System.currentTimeMillis(), -1);
        punishment.save();

        publishStaffChat(punishment, sender, target.getName());
        JedisPublisher.create("punishments").set("punishmentType", PunishmentType.BAN.name()).set("target", target.getUuid().toString()).set("reason", reason).publish();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Babel.translate("banned_player").sendMessage(player, target.getName());

            noteSuggestion(punishment, player);
        } else {
            sender.sendMessage(ChatColor.GREEN + "Banned " + target.getName());
        }
    }

    @Command(names = {"tempban"}, requiredRank = Rank.ADMIN, hideFromHelp = true)
    public static void tempban(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Time", defaultValue = "1") String time, @Param(name = "Unit", defaultValue = "Hour") String unit, @Param(name = "Message", wildcard = true) String reason) {
        UUID source = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        long till = TimeUtils.getTimeInMS(time, unit);
        Punishment punishment = new Punishment(PunishmentType.TEMP_BAN, source, target.getUuid(), reason, System.currentTimeMillis(), System.currentTimeMillis() + till);
        punishment.save();

        publishStaffChat(punishment, sender, target.getName());
        JedisPublisher.create("punishments").set("punishmentType", PunishmentType.TEMP_BAN.name()).set("target", target.getUuid().toString()).set("reason", reason).publish();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Babel.translate("tempbanned_player").sendMessage(player, target.getName(), time, unit);

            noteSuggestion(punishment, player);
        } else {
            sender.sendMessage(ChatColor.GREEN + "Temp banned " + target.getName() + " for " + time + " " + unit);
        }
    }

    @Command(names = {"kick"}, requiredRank = Rank.HELPER)
    public static void kick(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Reason", wildcard = true) String reason) {
        UUID source = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        Punishment punishment = new Punishment(PunishmentType.KICK, source, target.getUuid(), reason, System.currentTimeMillis(), -1);
        punishment.save();

        publishStaffChat(punishment, sender, target.getName());
        JedisPublisher.create("punishments").set("punishmentType", PunishmentType.KICK.name()).set("target", target.getUuid().toString()).set("reason", reason).publish();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Babel.translate("kicked_player").sendMessage(player, target.getName());

            noteSuggestion(punishment, player);
        } else {
            sender.sendMessage(ChatColor.GREEN + "Kicked " + target.getName());
        }
    }

    @Command(names = {"unban"}, requiredRank = Rank.MODERATOR, async = true)
    public static void unban(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target) {
        MongoCollection<Document> punishments = MongoManager.getInstance().getCollection("punishments");
        punishments.updateMany(Filters.eq("target", target.getUuid().toString()), new Document("$set", new Document("undone", true)));

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Babel.translate("successful_unban").sendMessage(player, target.getName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Unbanned " + target.getName());
        }
    }

    @Command(names = {"unmute"}, requiredRank = Rank.MODERATOR, async = true)
    public static void unmute(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target) {
        MongoCollection<Document> punishments = MongoManager.getInstance().getCollection("punishments");
        punishments.updateMany(Filters.and(Filters.eq("target", target.getUuid().toString()), Filters.eq("type", "MUTE")), new Document("$set", new Document("undone", true)));

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Babel.translate("successful_unmute").sendMessage(player, target.getName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Unmute " + target.getName());
        }
    }

    @Command(names = {"addnote"}, requiredRank = Rank.HELPER, async = true)
    public static void addNote(Player player, @Param(name = "Punishment ID") String id) {
        MongoCollection<Document> punishments = MongoManager.getInstance().getCollection("punishments");
        Scheduler.runAsync(() -> punishments.find(Filters.eq("punishmentId", id)).first(), new FutureCallback<Document>() {
            @Override
            public void onSuccess(@Nullable Document document) {
                if (document == null) {
                    Babel.translate("punishment_not_found").sendMessage(player, id);
                    return;
                }

                int slot = player.getInventory().firstEmpty();
                ItemStack stack = new ItemBuilder(Material.BOOK_AND_QUILL).displayName(ChatColor.GRAY + "Note " + ChatColor.GOLD + "#" + id).build();
                player.getInventory().setItem(slot, stack);
                player.getInventory().setHeldItemSlot(slot);

                Babel.translate("open_note").sendMessage(player);

                Bukkit.getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void on(PlayerEditBookEvent event) {
                        if (Objects.equals(event.getPlayer().getUniqueId(), player.getUniqueId())) {
                            punishments.updateOne(document,
                                    new Document("$set", new Document("notes", event.getNewBookMeta().getPages())));

                            HandlerList.unregisterAll(this);

                            Babel.translate("note_added").sendMessage(player);

                            player.getInventory().setItem(slot, null);
                        }
                    }
                }, FeatherPlugin.get());
            }

            @Override
            public void onFailure(Throwable throwable) {
                Babel.translate("punishment_not_found").sendMessage(player, id);
                throwable.printStackTrace();
            }
        });
    }

    @Command(names = {"viewhistory"}, requiredRank = Rank.HELPER, async = true)
    public static void viewHistory(Player player, @Param(name = "Target") UUIDNameKeypair target) {
        NetworkPlayer npTarget;
        Player pTarget;
        boolean shouldRemove = false;

        if ((pTarget = Bukkit.getPlayer(target.getUuid())) != null) {
            npTarget = NetworkPlayer.of(pTarget);
        } else {
            npTarget = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
            shouldRemove = true;
        }

        if (npTarget.getLastKnownUsername() == null) {
            Babel.translate("player_not_played_before").sendMessage(player, target.getName());
            return;
        }

        MongoCollection<Document> punishments = MongoManager.getInstance().getCollection("punishments");
        boolean finalShouldRemove = shouldRemove;
        Scheduler.runAsync(() -> punishments.find(Filters.eq("target", target.getUuid().toString())).sort(new Document("timestamp", -1)), new FutureCallback<FindIterable<Document>>() {
            @Override
            public void onSuccess(@Nullable FindIterable<Document> documents) {
                if (documents == null) {
                    Babel.translate("no_history").sendMessage(player, target.getName());
                    return;
                }

                InventoryGUI gui = new InventoryGUI("Punishment History", 54, FeatherPlugin.get());
                border(gui, new ItemStack(Material.STAINED_GLASS, 1, DyeColor.LIGHT_BLUE.getWoolData()));

                gui.setItem(0, new ItemBuilder(Material.SKULL_ITEM).durability((short) SkullType.PLAYER.ordinal()).owner(target.getName()).lore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Rank: " + (npTarget.getRank() == Rank.MEMBER ? "Member" : npTarget.getRank().getColoredTag()),
                        ChatColor.DARK_GRAY + "Level: " + ChatColor.GOLD + npTarget.getLevel(),
                        ChatColor.DARK_GRAY + "UUID: " + ChatColor.GOLD + npTarget.getUuid().toString(),
                        ChatColor.DARK_GRAY + "First Join: " + ChatColor.GOLD + new Date(npTarget.getFirstJoin()),
                        ChatColor.DARK_GRAY + "Last Join: " + ChatColor.GOLD + new Date(npTarget.getLastJoin())
                )).displayName(ChatColor.YELLOW + target.getName()), player -> {
                });

                int index = 0;
                for (Document document : documents) {
                    List<String> lore = new ArrayList<>();
                    lore.addAll(Arrays.asList(ChatColor.DARK_GRAY + "Time: " + ChatColor.RESET + new Date(document.getLong("timestamp")),
                            ChatColor.DARK_GRAY + "Undone: " + ChatColor.RESET + document.getBoolean("undone"),
                            ChatColor.DARK_GRAY + "Reason: " + ChatColor.RESET + document.getString("reason"),
                            ChatColor.DARK_GRAY + "Appeal Code: " + ChatColor.RESET + document.getString("appealCode"),
                            " ",
                            ChatColor.DARK_GRAY + "Has Notes? " + (document.containsKey("notes") ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No")));

                    long untilLong = document.getLong("until");
                    String until = ChatColor.DARK_GRAY + "Until: " + (untilLong < System.currentTimeMillis() ? ChatColor.GREEN + "Finished" : (untilLong == -1 ? ChatColor.GOLD + "Instant" : ChatColor.RESET.toString() + new Date(untilLong)));
                    lore.add(1, until);

                    gui.setItem(mapped_menu_items[index++],
                            new ItemBuilder(Material.STAINED_CLAY).
                                    durability(getData(PunishmentType.valueOf(document.getString("type")))).
                                    lore(lore).displayName(displayName(document))
                            , player1 -> {
                                if (document.containsKey("notes")) {
                                    player1.closeInventory();

                                    for (String note : ((List<String>) document.get("notes"))) {
                                        player1.sendMessage(ChatColor.GRAY + "- " + ChatColor.RESET + note);
                                    }
                                } else {
                                    Babel.translate("no_notes_found").sendMessage(player1);
                                }
                            });
                }

                gui.open(player);
                if (finalShouldRemove) PlayerManager.getInstance().removePlayer(target.getUuid());
            }

            @Override
            public void onFailure(Throwable throwable) {
                Babel.translate("failed_to_find_history").sendMessage(player, target.getName());
            }
        });
    }

    private static String displayName(Document document) {
        PunishmentType type = PunishmentType.valueOf(document.getString("type"));
        String id = document.containsKey("punishmentId") ? document.getString("punishmentId") : "Unknown";

        return ChatColor.RED + type.name() + ChatColor.DARK_GRAY + " (" + ChatColor.GRAY + id + ChatColor.DARK_GRAY + ")";
    }

    private static short getData(PunishmentType type) {
        switch (type) {
            case MUTE:
                return DyeColor.GREEN.getWoolData();
            case KICK:
                return DyeColor.YELLOW.getWoolData();
            case TEMP_BAN:
            case H_TEMP_BAN:
                return DyeColor.RED.getWoolData();
            case BAN:
            case H_BAN:
                return DyeColor.BLACK.getWoolData();
        }

        return 0;
    }

    private static void border(InventoryGUI gui, ItemStack stack) {
        ItemBuilder builder = new ItemBuilder(stack);
        builder.displayName("");
        Consumer<Player> consumer = player -> {
        };

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, builder, consumer);
        }

        for (int i = 0; i < 54; i += 9) {
            gui.setItem(i, builder, consumer);
        }

        for (int i = 8; i < 54; i += 9) {
            gui.setItem(i, builder, consumer);
        }

        for (int i = 54 - 9; i < 54; i++) {
            gui.setItem(i, builder, consumer);
        }
    }

    private static void publishStaffChat(Punishment punishment, CommandSender sender, String targetName) {
        String actor;
        if (sender instanceof Player) {
            NetworkPlayer np = NetworkPlayer.of((Player) sender);
            actor = np.getRank().getColor() + np.getPlayer().getName();
        } else {
            actor = ChatColor.RED + "Minespree";
        }

        String action;
        switch (punishment.getType()) {
            case KICK:
                action = "kicked";
                break;
            case BAN:
            case H_BAN:
                action = "permanently banned";
                break;
            case MUTE:
                action = "muted";
                break;
            case TEMP_BAN:
            case H_TEMP_BAN:
                action = "temporarily banned";
                break;
            default:
                action = "punished";
        }

        String text = actor + ChatColor.GRAY + " " + action + " " + ChatColor.RED + targetName + ChatColor.GRAY + " for " + punishment.getReason();
        JedisPublisher.create("staff-chat").set("message", text).publish();
    }

    private static void noteSuggestion(Punishment punishment, Player player) {
        String message = Babel.translate("punishment_note_suggestion").toString(player, punishment.getId());

        ComponentBuilder builder = new ComponentBuilder(message)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/addnote " + punishment.getId()))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.GRAY + "Click to add a punishment note.")));

        player.spigot().sendMessage(builder.create());
    }

}
