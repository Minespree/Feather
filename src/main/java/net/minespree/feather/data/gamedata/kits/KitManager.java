package net.minespree.feather.data.gamedata.kits;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelStringMessageType;
import net.minespree.babel.MultiBabelMessage;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.implementations.KittedPlayer;
import net.minespree.feather.util.RomanNumeral;
import net.minespree.feather.util.Scheduler;
import net.minespree.wizard.gui.AuthenticationGUI;
import net.minespree.wizard.gui.PerPlayerInventoryGUI;
import net.minespree.wizard.util.ItemBuilder;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class KitManager {

    private static final BabelStringMessageType EQUIPPED = Babel.translate("equipped");
    private static final BabelStringMessageType OWNED = Babel.translate("owned");
    private static final BabelStringMessageType CLICK_TO_EQUIP = Babel.translate("left_click_equip");
    private static final BabelStringMessageType RIGHT_CLICK_BUY = Babel.translate("right_click_buy");
    private static final BabelStringMessageType COSTS_COINS = Babel.translate("costs_coins");

    @Getter
    private final static KitManager instance = new KitManager();

    private Map<GameRegistry.Type, PerPlayerInventoryGUI> menus = Maps.newHashMap();
    private boolean loaded;
    private Set<String> games = Sets.newHashSet();
    private Map<GameRegistry.Type, List<Kit>> loadedKits = Maps.newHashMap();

    public Kit getKit(GameRegistry.Type game, String kitId) {
        return loadedKits.get(game).stream().filter(kit -> kit.getKitId().equals(kitId)).findAny().orElse(null);
    }

    private void addKit(GameRegistry.Type game, Kit kit) {
        if (loadedKits.containsKey(game))
            loadedKits.get(game).add(kit);
        else {
            loadedKits.put(game, Lists.newArrayList());
            loadedKits.get(game).add(kit);
        }
    }

    public void open(Player player, GameRegistry.Type game) {
        Preconditions.checkArgument(menus.containsKey(game));

        menus.get(game).open(player);
    }

    public void load() {
        Scheduler.run(() -> MongoManager.getInstance().getCollection("kits"), new FutureCallback<MongoCollection<Document>>() {
            @Override
            public void onSuccess(MongoCollection<Document> documentMongoCollection) {
                FindIterable<Document> fi = documentMongoCollection.find();
                for (Document document : fi) {
                    load(document);
                }
                loaded = true;
            }

            @Override
            public void onFailure(Throwable throwable) {
                try {
                    throw throwable;
                } catch (Throwable throwable1) {
                    throwable1.printStackTrace();
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void load(Document document) {

        GameRegistry.Type game = GameRegistry.Type.valueOf(document.getString("_id").toUpperCase());
        String menu = document.getString("menu");
        int size = document.getInteger("size");
        List<Document> kits = (List<Document>) document.get("kits");

        PerPlayerInventoryGUI kitMenu = new PerPlayerInventoryGUI(Babel.translate(menu), size, FeatherPlugin.get());

        for (Document kit : kits) {
            BabelStringMessageType name = Babel.translate(kit.getString("name"));
            String id = kit.getString("id");
            ItemBuilder kitItem = new ItemBuilder((Document) kit.get("item"));

            Document tiersDocument = (Document) kit.get("tiers");
            Tier[] tiers = new Tier[tiersDocument.size()];

            for (String num : tiersDocument.keySet()) {
                Document tierDocument = (Document) tiersDocument.get(num);
                Map<Integer, ItemBuilder> inventory = new HashMap<>(), armour = new HashMap<>();
                Map<String, Document> data = new HashMap<>();
                MultiBabelMessage description = Babel.translateMulti(tierDocument.getString("description"));

                if (tierDocument.containsKey("inventory")) {
                    Document inventoryDocument = (Document) tierDocument.get("inventory");
                    for (String strSlot : inventoryDocument.keySet()) {
                        inventory.put(Integer.parseInt(strSlot), new ItemBuilder((Document) inventoryDocument.get(strSlot)));
                    }
                }

                if (tierDocument.containsKey("armour")) {
                    Document armourDocument = (Document) tierDocument.get("armour");
                    for (String strSlot : armourDocument.keySet()) {
                        ItemBuilder item = new ItemBuilder((Document) armourDocument.get(strSlot));
                        switch (strSlot) {
                            case "helmet":
                                armour.put(0, item);
                                break;
                            case "chestplate":
                                armour.put(1, item);
                                break;
                            case "leggings":
                                armour.put(2, item);
                                break;
                            case "boots":
                                armour.put(3, item);
                                break;
                            default:
                                break;
                        }
                    }
                }

                if (tierDocument.containsKey("data"))
                    data.putAll((Map<String, Document>) tierDocument.get("data"));
                tiers[Integer.parseInt(num)] = new Tier(description, inventory, armour, data, tierDocument.getInteger("price"), tierDocument.getInteger("slot"));
            }

            Boolean defKit = kit.getBoolean("defaultKit");
            PerPlayerInventoryGUI tierGui = new PerPlayerInventoryGUI(Babel.translate("tier_upgrade_title"), 45, FeatherPlugin.get());

            Kit k = new Kit(id, tiers, defKit, tierGui);

            for (int t = 0; t < tiers.length; t++) {
                int i = t;
                Tier tier = tiers[t];
                tierGui.setItem(40, p -> new ItemBuilder(Material.BOOK).displayName(AuthenticationGUI.back).lore(AuthenticationGUI.back_lore).build(p),
                        (p, clickType) -> kitMenu.open(p));

                tierGui.setItem(tier.getSlot(), player -> {
                    KittedPlayer kp = getPlayer(player);
                    PlayerKit pk = kp.getPlayerKit(game, id);
                    ItemBuilder builder = new ItemBuilder(Material.STAINED_CLAY).displayName(name.toString(player, RomanNumeral.toRoman(i + 1))).lore(tier.getDescription());
                    builder.lore("");

                    if (pk == null || i > pk.getCurrentTier()) {
                        builder.lore(Babel.translate("costs_coins").toString(player, tier.getPrice()));
                        builder.lore(Babel.translate("click_to_upgrade"));
                    } else builder.lore(Babel.translate("tier_unlocked"));
                    if (pk != null && pk.getCurrentTier() >= i) {
                        builder.durability((short) 5);
                    } else if ((pk == null && i == 0) || (pk != null && pk.getCurrentTier() + 1 == i)) {
                        builder.durability((short) 4);
                    } else {
                        builder.durability((short) 14);
                    }
                    return builder.build(player);
                }, (player, type) -> {
                    KittedPlayer kp = getPlayer(player);
                    PlayerKit pk = kp.getPlayerKit(game, id);

                    if ((pk == null && i == 0) || (pk != null && pk.getCurrentTier() == i - 1)) {
                        if (pk == null || pk.getCurrentTier() < tiers.length - 1) {
                            int price = pk == null ? k.getTiers()[0].getPrice() : k.getTiers()[pk.getCurrentTier() + 1].getPrice();
                            AuthenticationGUI.authenticate(player, Babel.translate("kit_purchase"), tierGui, kitItem.lore(tier.getDescription()), p -> {
                                if (kp.getCoins() >= price) {
                                    kp.removeCoins(price);
                                    if (kp.getDefaultKit(game) != null) {
                                        kp.getDefaultKit(game).setDefaultKit(false);
                                    }
                                    PlayerKit playerKit = pk;

                                    if (pk == null) {
                                        playerKit = new PlayerKit(k, 0, k.getTiers()[0], true);

                                        kp.addKit(game, playerKit);
                                    } else {
                                        playerKit.setTier(tiers[i]);
                                        playerKit.setCurrentTier(i);
                                        playerKit.setDefaultKit(true);

                                        kp.updateTier(game, playerKit);
                                    }

                                    kp.setDefaultKit(game, playerKit);
                                    tierGui.open(player);
                                } else {
                                    player.closeInventory();
                                    Babel.translate("cant_afford").sendMessage(player);
                                }
                            }, p -> tierGui.open(player));
                        }
                    }
                });
            }

            kitMenu.setItem(kit.getInteger("slot"), player -> {
                kitItem.clearLore();
                KittedPlayer kittedPlayer = getPlayer(player);
                PlayerKit playerKit = kittedPlayer.getPlayerKit(game, k.getKitId());
                int currentTier = playerKit == null ? 0 : playerKit.getCurrentTier();

                kitItem.displayName(name, RomanNumeral.toRoman(currentTier + 1));
                if (playerKit != null) {
                    if (playerKit.isDefaultKit()) {
                        kitItem.glow();
                    } else {
                        kitItem.unglow();
                    }
                    kitItem.lore(k.getTiers()[playerKit.getCurrentTier()].getDescription());
                }
                if (currentTier < k.getTiers().length - 1) {
                    kitItem.lore(" ");
                    kitItem.lore(COSTS_COINS, k.getTiers()[playerKit == null ? 0 : currentTier + 1].getPrice());
                    kitItem.lore(RIGHT_CLICK_BUY);
                }
                if (playerKit != null) {
                    kitItem.lore(" ");
                    if (!playerKit.isDefaultKit()) {
                        kitItem.lore(OWNED);
                        kitItem.lore(CLICK_TO_EQUIP);
                    } else {
                        kitItem.lore(EQUIPPED);
                    }
                }
                return kitItem.build(player);
            }, (player, type) -> {
                if (type.isLeftClick()) {
                    KittedPlayer kittedPlayer = getPlayer(player);
                    PlayerKit playerKit = kittedPlayer.getPlayerKit(game, k.getKitId());
                    if (playerKit != null) {
                        playerKit.setDefaultKit(true);
                        kittedPlayer.setDefaultKit(game, playerKit);
                        kitMenu.refresh(player);
                    }
                } else {
                    tierGui.open(player);
                }
            });

            menus.put(game, kitMenu);

            addKit(game, k);
        }
    }

    public KittedPlayer getPlayer(Player player) {
        return (KittedPlayer) PlayerManager.getInstance().getPlayer(player);
    }

}
