package net.minespree.feather.data.gamedata.perks;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.player.perks.PlayerPerks;
import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.types.IntegerType;
import net.minespree.feather.repository.types.Type;
import net.minespree.wizard.util.ItemBuilder;

import java.util.List;

@Getter
public class Perk implements Element {

    private static final IntegerType DEFAULT_TYPE = new IntegerType();

    private final GameRegistry.Type game;
    private final String id;
    private final int maxLevel;
    PerkExtension extension;
    private int lowestFreeLevel;
    private List<Integer> levelPrices;
    private ItemBuilder menuItem;

    public Perk(GameRegistry.Type game, String id, List<Integer> levelPrices, ItemBuilder menuItem) {
        this.game = game;
        this.id = id;
        this.maxLevel = levelPrices.size();
        this.levelPrices = levelPrices;
        this.menuItem = menuItem;

        for (int i = 0; i < levelPrices.size(); i++) {
            if (levelPrices.get(i) > 0) {
                continue;
            }
            lowestFreeLevel = i + 1;
            break;
        }

        PlayerPerks.getRegistry().register(this);
    }

    public int getPrice(int level) {
        Preconditions.checkElementIndex(level - 1, levelPrices.size(), "Level " + (level - 1) + " is outside the level bounds");
        return levelPrices.get(level - 1);
    }

    @Override
    public Type getType() {
        return DEFAULT_TYPE;
    }

    @Override
    public Object getDefaultValue() {
        return lowestFreeLevel;
    }

    @Override
    public boolean hasDefaultValue() {
        return lowestFreeLevel > 0;
    }
}
