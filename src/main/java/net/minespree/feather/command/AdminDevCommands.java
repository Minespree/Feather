package net.minespree.feather.command;

import com.google.common.collect.Maps;
import net.minespree.babel.Babel;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.command.system.CommandManager;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.data.whitelist.WhitelistMode;
import net.minespree.feather.db.redis.RedisManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.RestartManager;
import net.minespree.feather.util.UUIDNameKeypair;
import net.minespree.wizard.particle.ParticleType;
import net.minespree.wizard.util.Chat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AdminDevCommands {

    private static ItemStack fish;
    private static Listener fishListener;
    private static Map<UUID, Long> fishCooldown = Maps.newHashMap();

    static {
        fish = new ItemStack(Material.RAW_FISH, 1);
        fish.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 3);
        fish.addUnsafeEnchantment(Enchantment.KNOCKBACK, 5);
        ItemMeta meta = fish.getItemMeta();
        meta.setDisplayName(Chat.AQUA + "Super Fish");
        meta.spigot().setUnbreakable(true);
        fish.setItemMeta(meta);
    }

    public static void registerEnum() {
        CommandManager.getInstance().registerTransformer(WhitelistMode.class, (sender, source) -> {
            try {
                return WhitelistMode.valueOf(source.toUpperCase());
            } catch (Exception e) {
                sender.sendMessage(Chat.RED + "Possibilities: " + StringUtils.join(WhitelistMode.values(), ","));
                return null;
            }
        });
    }

    @Command(names = {"whitelist"}, requiredRank = Rank.ADMIN, hideFromHelp = true)
    public static void whitelistHelp(Player player) {
        player.sendMessage(Chat.WHITE + "/whitelist set");
        player.sendMessage(Chat.WHITE + "/whitelist check");
    }

    @SuppressWarnings("unchecked")
    @Command(names = {"whitelist set"}, requiredRank = Rank.ADMIN, async = true, hideFromHelp = true)
    public static void whitelist(Player player, @Param(name = "Mode") WhitelistMode mode) {
        try (Jedis jedis = RedisManager.getInstance().getPool().getResource()) {
            JSONObject object = new JSONObject();
            object.put("executer", player.getUniqueId().toString());
            object.put("mode", mode.name());

            jedis.set("whitelist", object.toJSONString());

            FeatherPlugin.get().getStaffChat().publishToStaff(player.getName() + " set whitelist to " + object.toJSONString());
        }
    }

    @Command(names = {"whitelist check"}, requiredRank = Rank.ADMIN, async = true, hideFromHelp = true)
    public static void whitelistCheck(Player player) {
        try (Jedis jedis = RedisManager.getInstance().getPool().getResource()) {
            String s = jedis.get("whitelist");
            if (s != null) {
                try {
                    JSONObject object = (JSONObject) new JSONParser().parse(s);
                    player.sendMessage(object.toJSONString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                player.sendMessage(Chat.RED + "No whitelist in redis");
            }
        }
    }

    @Command(names = {"prefix"}, requiredRank = Rank.ADMIN)
    public static void prefix(CommandSender sender) {
        sender.sendMessage("/prefix set");
        sender.sendMessage("/prefix clear");
    }

    @Command(names = {"prefix set"}, requiredRank = Rank.ADMIN, async = true)
    public static void prefixSet(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Prefix", wildcard = true) String prefix) {
        Player p = Bukkit.getPlayer(target.getUuid());
        prefix = ChatColor.translateAlternateColorCodes('&', prefix);
        prefix += " ";

        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);
            np.setPrefix(prefix);

            sender.sendMessage(ChatColor.GREEN + "Successfully set " + target.getName() + "'s prefix to " + prefix);
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            data.setPrefix(prefix);

            sender.sendMessage(ChatColor.GREEN + "Successfully set " + target.getName() + "'s prefix to " + prefix);

            PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            sender.sendMessage(ChatColor.RED + "That player is not in the database.");
        }
    }

    @Command(names = {"prefix clear"}, requiredRank = Rank.ADMIN, async = true)
    public static void clearPrefix(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target) {
        Player p = Bukkit.getPlayer(target.getUuid());

        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);
            np.setPrefix(null);

            sender.sendMessage(ChatColor.GREEN + "Successfully cleared " + target.getName() + "'s prefix");
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            data.setPrefix(null);

            sender.sendMessage(ChatColor.GREEN + "Successfully cleared " + target.getName() + "'s prefix");

            PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            sender.sendMessage(ChatColor.RED + "That player is not in the database.");
        }
    }

    @Command(names = {"announce"}, requiredRank = Rank.ADMIN, async = true)
    public static void announce(Player player, @Param(wildcard = true, name = "Message") String message) {
        JSONObject o = new JSONObject();
        o.put("channel", "announce");
        o.put("message", message);
        RedisManager.getInstance().post(o);
    }

    @Command(names = {"coins give"}, requiredRank = Rank.ADMIN, async = true)
    public static void giveCoins(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Amount") int amount) {
        Player p = Bukkit.getPlayer(target.getUuid());

        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);
            np.addCoins(amount);

            sender.sendMessage(ChatColor.GREEN + "Successfully gave " + amount + " coins to " + target.getName() + ".");
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            data.addCoins(amount);

            sender.sendMessage(ChatColor.GREEN + "Successfully gave " + amount + " coins to " + target.getName() + ".");

            PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            sender.sendMessage(ChatColor.RED + "That player is not in the database.");
        }
    }

    @Command(names = {"coins take"}, requiredRank = Rank.ADMIN, async = true)
    public static void takeCoins(CommandSender sender, @Param(name = "Target") UUIDNameKeypair target, @Param(name = "Amount") int amount) {
        Player p = Bukkit.getPlayer(target.getUuid());

        if (p != null) {
            NetworkPlayer np = PlayerManager.getInstance().getPlayer(p);
            np.removeCoins(amount);

            sender.sendMessage(ChatColor.GREEN + "Successfully took " + amount + " coins to " + target.getName() + ".");
            return;
        }

        NetworkPlayer data = PlayerManager.getInstance().getPlayer(target.getUuid(), true);
        if (data != null) {
            data.removeCoins(amount);

            sender.sendMessage(ChatColor.GREEN + "Successfully took " + amount + " coins to " + target.getName() + ".");

            PlayerManager.getInstance().removePlayer(target.getUuid());
        } else {
            sender.sendMessage(ChatColor.RED + "That player is not in the database.");
        }
    }

    @Command(names = {"tpall"}, requiredRank = Rank.ADMIN)
    public static void tpAll(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) continue;
            other.teleport(player);
        }
        Babel.translate("teleported_all").sendMessage(player);
    }

    @Command(names = {"tpall world", "tpaw"}, requiredRank = Rank.ADMIN)
    public static void tpAllWorld(Player player) {
        for (Player p : player.getWorld().getPlayers()) {
            if (p == player) continue;
            p.teleport(player);
        }
        Babel.translate("teleported_all_world").sendMessage(player);
    }

    @Command(names = {"rocket"}, requiredRank = Rank.ADMIN)
    public static void rocket(Player player, @Param(name = "Target", defaultValue = "all") String target) {
        if ("all".equalsIgnoreCase(target)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Chat.GREEN + "You launched " + p.getName() + " on a rocket!");
                rocket(p);
            }
        } else {
            Player p = Bukkit.matchPlayer(target).stream().findFirst().orElse(null);
            if (p == null) {
                player.sendMessage(Chat.RED + target + " is not online.");
            } else {
                player.sendMessage(Chat.GREEN + "You launched " + p.getName() + " on a rocket!");
                rocket(p);
            }
        }
    }

    @Command(names = {"superfish"}, requiredRank = Rank.ADMIN)
    public static void superfish(Player player, @Param(name = "Target", defaultValue = "self") String target) {
        if ("all".equalsIgnoreCase(target)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getInventory().contains(fish)) continue;
                p.getInventory().addItem(fish);
                p.sendMessage(Chat.GREEN + "You got a " + Chat.AQUA + "Super Fish " + Chat.GREEN + "from " + player.getName());
            }
            player.sendMessage(Chat.GREEN + "You gave everyone a super fish.");

            if (fishListener == null) {
                fishListener = new Listener() {
                    @EventHandler
                    public void on(PlayerInteractEvent event) {
                        if (event.hasItem() && event.getItem().equals(fish)) {
                            if (fishCooldown.getOrDefault(event.getPlayer().getUniqueId(), 0L) <= System.currentTimeMillis()) {
                                event.getPlayer().getWorld().strikeLightningEffect(event.getPlayer().getLocation());
                                fishCooldown.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
                                event.getPlayer().sendMessage(Chat.GREEN + "The fish goes skrrrrra, pa pa ka ka ka");
                            } else {
                                event.getPlayer().sendMessage(Chat.RED + "Superfish is on cooldown!");
                            }
                        }
                    }
                };
            }

            FeatherPlugin.get().getServer().getPluginManager().registerEvents(fishListener, FeatherPlugin.get());
        } else if ("take".equalsIgnoreCase(target)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().remove(fish);
                p.sendMessage(Chat.RED + "Your superfish was taken away by " + player.getName());
            }
            player.sendMessage(Chat.GREEN + "You removed everyone's superfish");
            if (fishListener != null) {
                HandlerList.unregisterAll(fishListener);
                fishListener = null;
            }
        } else if ("self".equalsIgnoreCase(target)) {
            player.getInventory().addItem(fish);
            player.sendMessage(Chat.GREEN + "You got a superfish.");
        } else {
            player.sendMessage("/superfish <all|take|self>");
        }
    }

    @Command(names = "effectall", requiredRank = Rank.ADMIN)
    public static void effectAll(Player player, @Param(name = "Effect") String effect, @Param(name = "Time") int time, @Param(name = "Multiplier") int multiplier) {
        PotionEffectType type = PotionEffectType.getByName(effect);

        if (type == null) {
            player.sendMessage(Chat.RED + "Unknown potion effect type");
            return;
        }

        Bukkit.getOnlinePlayers().forEach(player1 -> {
            player1.addPotionEffect(new PotionEffect(type, time, multiplier));
        });

        player.sendMessage(Chat.GREEN + "Effect all successfull!");
    }

    @Command(names = {"scheduledeprovision", "remoteshutdown", "killitwithfire"}, requiredRank = Rank.ADMIN)
    public static void scheduleDeprovision(CommandSender sender, @Param(name = "Matcher") String target) {
        RestartManager manager = FeatherPlugin.get().getRestartManager();
        manager.scheduleRestartForType(target);

        sender.sendMessage(Chat.GREEN + "Scheduled deprovision for all instances matching \'" + target + "\'");
    }

    private static void rocket(Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(100);
        meta.clearEffects();
        firework.setFireworkMeta(meta);

        firework.setPassenger(player);

        player.sendMessage(Chat.GREEN + "Up to the moon you go!");
        player.playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, 1F, 1F);

        //squid.setVelocity(new Vector(0, 5, 0));

        new BukkitRunnable() {
            int ran = 0;

            @Override
            public void run() {
                if (firework.getPassenger() == null || !firework.isValid() || !player.isOnline()) {
                    firework.remove();
                    cancel();
                    return;
                }

                ran++;

                ParticleType.CLOUD.display(null, firework.getLocation(), null, 16, 0, 0, 0, 1, 5, Bukkit.getOnlinePlayers());

                if (ran >= 50) {
                    firework.remove();
                    cancel();
                }
            }
        }.runTaskTimer(FeatherPlugin.get(), 0L, 1L);
    }
}
