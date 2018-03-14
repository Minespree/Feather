package net.minespree.feather.data.gamedata.perks;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerKey;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.perks.PlayerPerkSet;
import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.util.Scheduler;
import net.minespree.wizard.gui.PerPlayerInventoryGUI;
import net.minespree.wizard.util.Chat;
import net.minespree.wizard.util.ItemBuilder;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PerkHandler {

    private static final BabelMessage PERK_SETS = Babel.translate("perks_sets");

    private static final BabelMessage EXCLUSIVE_RANK = Babel.translate("exclusive_rank");
    private static final BabelMessage OWNED = Babel.translate("owned");
    private static final BabelMessage EQUIP_CLICK = Babel.translate("perks_equip_click");

    private static final BabelMessage EQUIPPED = Babel.translate("equipped");
    private static final BabelMessage REMOVE_CLICK = Babel.translate("perks_remove_click");

    private static final BabelMessage EDIT = Babel.translate("perks_edit");
    private static final BabelMessage EDIT_CLICK = Babel.translate("perks_edit_click");

    @Getter
    private static PerkHandler instance = new PerkHandler();

    @Getter
    private Map<String, Perk> perks = Maps.newHashMap();
    private Map<GameRegistry.Type, Integer> maxPerksSelected = Maps.newHashMap();

    @Getter
    private Map<GameRegistry.Type, PerPlayerInventoryGUI> guis = Maps.newHashMap();

    private PerkHandler() {
    }

    public void load() {
        Scheduler.run(() -> MongoManager.getInstance().getCollection("perks"), new FutureCallback<MongoCollection<Document>>() {
            @Override
            public void onSuccess(MongoCollection<Document> documentMongoCollection) {
                FindIterable<Document> fi = documentMongoCollection.find();
                for (Document document : fi) {
                    load(document);
                }
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

    public void load(Document document) {
        GameRegistry.Type game = GameRegistry.Type.valueOf(document.getString("_id"));
        int maxSelected = document.getInteger("maxSelected");
        String key = PlayerKey.SETS + "." + game.name();

        PerPlayerInventoryGUI gui = new PerPlayerInventoryGUI(Babel.translate(document.getString("title")), 45, FeatherPlugin.get());
        for (int i = 0; i < 5; i++) {
            int finalI = i + 1;
            gui.setItem(11 + i, p -> {
                NetworkPlayer player = NetworkPlayer.of(p);
                PerkSet set = player.getPerks().getPerkSet(game).getPerkSet(finalI);
                boolean has = hasSet(player, finalI);

                ItemBuilder builder = new ItemBuilder(has ? Material.BOOK : Material.BARRIER).displayName(PERK_SETS, finalI);
                if (has) {
                    if (set != null && set.getSelectedPerks().size() > 0) {
                        builder.lore(" ");
                        for (Perk perk : set.getSelectedPerks()) {
                            builder.lore(perk.getMenuItem().getDisplayName().getType());
                        }
                    }
                    builder.lore(" ");
                    if (set == null) {
                        builder.lore(Chat.RED + "0/" + maxSelected);
                    } else {
                        builder.lore(Chat.GREEN + set.getSelectedPerks().size() + "/" + maxSelected);
                    }
                    builder.lore(" ");
                    if (finalI != player.getPerks().getDefaultSet(game)) {
                        builder.lore(OWNED);
                        builder.lore(EQUIP_CLICK);
                    } else {
                        builder.lore(EQUIPPED);
                        builder.lore(REMOVE_CLICK);
                    }
                    builder.lore(" ");
                    builder.lore(EDIT);
                    builder.lore(EDIT_CLICK);
                } else {
                    builder.lore(" ");
                    Rank rank = null;
                    switch (finalI) {
                        case 3:
                            rank = Rank.IRON;
                            break;
                        case 4:
                            rank = Rank.GOLD;
                            break;
                        case 5:
                            rank = Rank.DIAMOND;
                            break;
                        default:
                            break;
                    }
                    if (rank != null) {
                        builder.lore(EXCLUSIVE_RANK, rank.getColor() + rank.getTag());
                    }
                }

                if (has && finalI == player.getPerks().getDefaultSet(game)) {
                    builder.material(Material.ENCHANTED_BOOK);
                    builder.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                return builder.build(p);
            }, (p, type) -> {
                NetworkPlayer player = NetworkPlayer.of(p);
                if (hasSet(player, finalI)) {
                    if (type.isLeftClick()) {
                        if (player.getPerks().getDefaultSet(game) == finalI) {
                            player.getPerks().getDefaultSets().remove(game);

                            player.addSaveOperation("$unset", new Document(key + ".defaultSet", 1));
                            PlayerManager.getInstance().attemptSave(player, true);
                        } else {
                            player.getPerks().getDefaultSets().put(game, finalI);

                            player.addUpdate(key + ".defaultSet", finalI);
                            PlayerManager.getInstance().attemptSave(player, true);
                        }
                        gui.refresh(p);
                    } else {
                        PlayerPerkSet sets = player.getPerks().getPerkSet(game);
                        PerkSet set = sets.getPerkSet(finalI);
                        if (set != null) {
                            set.openEditGui(gui);
                        } else {
                            sets.createSet(finalI).openEditGui(gui);
                        }
                    }
                }
            });
        }

        List<Document> perkDocs = (List<Document>) document.get("perks");
        for (Document perkDoc : perkDocs) {
            Perk perk = new Perk(game, perkDoc.getString("id"), (List<Integer>) perkDoc.get("levels"), new ItemBuilder((Document) perkDoc.get("menuItem")));

            perks.put(perk.getId(), perk);
        }
        maxPerksSelected.put(game, maxSelected);
        guis.put(game, gui);
    }

    public void open(Player player, GameRegistry.Type game) {
        Preconditions.checkNotNull(game, "Game can't be null");
        Preconditions.checkArgument(guis.containsKey(game), "No decks for " + game.name());
        guis.get(game).open(player);
    }

    private boolean hasSet(NetworkPlayer player, int set) {
        return set <= 2 || (set == 3 && player.getRank().has(Rank.IRON)) || (set == 4 && player.getRank().has(Rank.GOLD)) || (set == 5 && player.getRank().has(Rank.DIAMOND));
    }

    public void setExtension(String perkId, PerkExtension extension) {
        Perk perk = getPerk(perkId);
        if (perk != null) {
            perk.extension = extension;
        }
    }

    public boolean hasPerks(GameRegistry.Type game) {
        return guis.containsKey(game);
    }

    public int getMaxSelected(GameRegistry.Type game) {
        Preconditions.checkArgument(maxPerksSelected.containsKey(game), "Game does not have perks");
        return maxPerksSelected.get(game);
    }

    public Perk getPerk(String perkId) {
        Preconditions.checkNotNull(perkId, "perkId is null");
        Preconditions.checkArgument(perks.containsKey(perkId), "Invalid perkId " + perkId);
        return perks.get(perkId);
    }

    public List<Perk> getPerks(GameRegistry.Type game) {
        return perks.values().stream().filter(perk -> perk.getGame() == game).collect(Collectors.toList());
    }

}
