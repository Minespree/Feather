package net.minespree.feather.data.gamedata.perks;

import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.babel.ComplexBabelMessage;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerKey;
import net.minespree.feather.player.PlayerManager;
import net.minespree.wizard.gui.AuthenticationGUI;
import net.minespree.wizard.gui.GUI;
import net.minespree.wizard.gui.MultiPageGUI;
import net.minespree.wizard.util.ItemBuilder;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PerkSet {

    private static final BabelMessage PERKS = Babel.translate("perks");
    private static final BabelMessage PERKS_TITLE = Babel.translate("perks_title");
    private static final BabelMessage ALREADY_ADDED = Babel.translate("perks_already_added");

    private static final BabelMessage OWNED = Babel.translate("owned");
    private static final BabelMessage EQUIP_CLICK = Babel.translate("perks_equip_click");

    private static final BabelMessage COSTS_COINS = Babel.translate("costs_coins");
    private static final BabelMessage PURCHASE_CLICK = Babel.translate("perks_purchase_click");
    private static final BabelMessage CLICK_UPGRADE = Babel.translate("perks_upgrade_click");

    private static final BabelMessage EQUIPPED = Babel.translate("equipped");
    private static final BabelMessage REMOVE_CLICK = Babel.translate("perks_remove_click");

    private static final BabelMessage MAX_SELECTED = Babel.translate("perks_max_selected");
    private static final BabelMessage GO_BACK = Babel.translate("go_back");
    private static final BabelMessage CANT_AFFORD = Babel.translate("cant_afford");
    private final String key;
    private NetworkPlayer networkPlayer;
    @Getter
    private List<Perk> selectedPerks;
    private GameRegistry.Type game;
    private int maxSelected, position;

    public PerkSet(NetworkPlayer networkPlayer, List<Perk> selectedPerks, GameRegistry.Type game, int position) {
        this.networkPlayer = networkPlayer;
        this.selectedPerks = selectedPerks;
        this.game = game;
        this.maxSelected = PerkHandler.getInstance().getMaxSelected(game);
        this.position = position;

        this.key = PlayerKey.SETS + "." + game.name() + "." + position;
    }

    public void openEditGui(GUI parent) {
        MultiPageGUI gui = new MultiPageGUI(new ComplexBabelMessage().append(PERKS_TITLE, position),
                "---------" +
                        "---------" +
                        "-XXXXXXX-" +
                        "-XXXXXXX-" +
                        "-XXXXXXX-", 54, 50, 48);

        ItemStack black = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        for (int i = 9; i < 18; i++) {
            gui.setItem(i, p -> black);
        }

        int i = 1;
        for (Perk perk : selectedPerks) {
            gui.setItem(i, p -> {
                ItemBuilder builder = perk.getMenuItem().clone();
                builder.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES);
                builder.lore(" ");
                builder.lore(EQUIPPED);
                builder.lore(REMOVE_CLICK);
                int level = (int) networkPlayer.getPerks().getValue(perk);
                if (level < perk.getMaxLevel()) {
                    builder.lore(" ");
                    builder.lore(COSTS_COINS, perk.getPrice(level + 1));
                    builder.lore(CLICK_UPGRADE);
                }
                return builder.build(p);
            }, (player, type) -> {
                if (type.isLeftClick()) {

                    Document unset = new Document(key + "." + selectedPerks.indexOf(perk), 1);
                    Document pull = new Document(key, null);

                    networkPlayer.addSaveOperation("$unset", unset);
                    PlayerManager.getInstance().getSaver().save(networkPlayer).whenComplete((saved, throwable) -> {
                        if (throwable != null) {
                            throwable.printStackTrace();
                        }
                        networkPlayer.addSaveOperation("$pull", pull);
                        PlayerManager.getInstance().attemptSave(networkPlayer, true);
                    });

                    selectedPerks.remove(perk);

                    openEditGui(parent);
                } else {
                    upgrade(gui, parent, perk);
                }
            });
            i++;
        }
        gui.setItem(0, new ItemBuilder(Material.BOOK).displayName(GO_BACK), (p, type) -> parent.open(p));

        for (Perk perk : PerkHandler.getInstance().getPerks(game)) {
            gui.addItem(p -> {
                ItemBuilder builder = perk.getMenuItem().clone().lore(" ");
                builder.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES);
                if (selectedPerks.contains(perk)) {
                    builder.glow();
                    builder.lore(ALREADY_ADDED);
                    int level = (int) networkPlayer.getPerks().getValue(perk);
                    if (level < perk.getMaxLevel()) {
                        builder.lore(" ");
                        builder.lore(COSTS_COINS, perk.getPrice(level + 1));
                        builder.lore(CLICK_UPGRADE);
                    }
                } else {
                    if (perk.hasDefaultValue() || networkPlayer.getPerks().hasValue(perk)) {
                        if (selectedPerks.size() < maxSelected) {
                            builder.lore(OWNED);
                            builder.lore(EQUIP_CLICK);
                        } else {
                            builder.lore(MAX_SELECTED);
                        }
                        int level = (int) networkPlayer.getPerks().getValue(perk);
                        if (level < perk.getMaxLevel()) {
                            builder.lore(" ");
                            builder.lore(COSTS_COINS, perk.getPrice(level + 1));
                            builder.lore(CLICK_UPGRADE);
                        }
                    } else {
                        builder.lore(COSTS_COINS, perk.getPrice(1));
                        builder.lore(PURCHASE_CLICK);
                    }
                }
                return builder.build(p);
            }, perk, (p, type) -> {
                if (type.isLeftClick()) {
                    if (!selectedPerks.contains(perk)) {
                        if (perk.hasDefaultValue() || networkPlayer.getPerks().hasValue(perk)) {
                            if (selectedPerks.size() < maxSelected) {
                                set(perk);

                                openEditGui(parent);
                            }
                        } else {
                            AuthenticationGUI.authenticate(networkPlayer.getPlayer(), PERKS, gui, perk.getMenuItem(), (player) -> {
                                if (networkPlayer.getCoins() >= perk.getPrice(1)) {
                                    networkPlayer.removeCoins(perk.getPrice(1));
                                    PlayerManager.getInstance().attemptSave(networkPlayer, true);

                                    networkPlayer.getPerks().setValue(perk, 1, true);

                                    set(perk);

                                    openEditGui(parent);
                                } else {
                                    CANT_AFFORD.sendMessage(player);
                                    gui.open(player);
                                }
                            }, gui::open);
                        }
                    }
                } else {
                    upgrade(gui, parent, perk);
                }
            });
        }
        gui.open(networkPlayer.getPlayer());
    }

    private void set(Perk perk) {
        if (selectedPerks.size() < maxSelected) {
            selectedPerks.add(perk);

            networkPlayer.addUpdate(key + "." + selectedPerks.indexOf(perk), perk.getId());
            PlayerManager.getInstance().attemptSave(networkPlayer, true);
        }
    }

    private void upgrade(GUI gui, GUI parent, Perk perk) {
        int level = (int) networkPlayer.getPerks().getValue(perk);
        if (level < perk.getMaxLevel()) {
            int nextLevel = level + 1;
            AuthenticationGUI.authenticate(networkPlayer.getPlayer(), PERKS, gui, perk.getMenuItem(), (player) -> {
                if (networkPlayer.getCoins() >= perk.getPrice(nextLevel)) {
                    networkPlayer.removeCoins(perk.getPrice(nextLevel));
                    networkPlayer.getPerks().setValue(perk, nextLevel, true);

                    PlayerManager.getInstance().attemptSave(networkPlayer, true);

                    openEditGui(parent);
                } else {
                    CANT_AFFORD.sendMessage(player);
                    gui.open(player);
                }
            }, gui::open);
        }
    }

}
