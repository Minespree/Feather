package net.minespree.feather.player.implementations;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.gamedata.kits.Kit;
import net.minespree.feather.data.gamedata.kits.KitManager;
import net.minespree.feather.data.gamedata.kits.PlayerKit;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerKey;
import org.bson.Document;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KittedPlayer extends NetworkPlayer {
    @Getter
    private Multimap<GameRegistry.Type, PlayerKit> unlockedKits;

    public KittedPlayer(UUID uuid) {
        super(uuid);
        this.unlockedKits = LinkedHashMultimap.create();
    }

    @Override
    public void bootstrap(Document document) {
        if (!isManagerLoaded()) {
            return;
        }

        Object kitsObj = document.get(PlayerKey.KITS);
        Document kitsDoc = kitsObj != null ? (Document) kitsObj : null;

        if (kitsDoc != null) {
            kitsDoc.forEach((type, kitObj) -> {
                GameRegistry.Type gameType = GameRegistry.Type.byId(type);

                if (gameType == null) {
                    return;
                }

                Document kitDoc = (Document) kitObj;

                String equippedId = kitDoc.getString("equipped");
                Document tiers = (Document) kitDoc.get("tiers");

                if (tiers.isEmpty()) {
                    return;
                }

                tiers.forEach((kitId, tierObj) -> {
                    boolean equipped = equippedId.equals(kitId);

                    Kit kit = KitManager.getInstance().getKit(gameType, kitId);

                    if (kit == null) return;

                    int tier = (int) tierObj;

                    PlayerKit playerKit = new PlayerKit(kit, tier, kit.getTiers()[tier], equipped);
                    unlockedKits.put(gameType, playerKit);
                });
            });
        }

        addDefaultKits();
    }

    /**
     * Adds default kits if they aren't present
     */
    private void addDefaultKits() {
        Map<GameRegistry.Type, List<Kit>> kits = KitManager.getInstance().getLoadedKits();

        kits.forEach((type, gameKits) -> {
            boolean hasDefault = getDefaultKit(type) != null;

            gameKits.forEach(kit -> {
                int lowestTier = lowestTierFree(kit);

                if (!kit.isDefaultKit() || lowestTier == -1 || hasKit(type, kit.getKitId())) {
                    return;
                }

                addInitialKit(type, lowestTier, kit, hasDefault);
            });

        });
    }

    private void addInitialKit(GameRegistry.Type type, int tier, Kit kit, boolean hasDefault) {
        PlayerKit playerKit = new PlayerKit(kit, tier, kit.getTiers()[tier], getDefaultKit(type) == null && kit.isDefaultKit());

        addKit(type, playerKit);

        if (!hasDefault && playerKit.isDefaultKit()) {
            // Equip default kit
            setDefaultKit(type, playerKit);
        }

        unlockedKits.put(type, playerKit);
    }

    public void updateTier(GameRegistry.Type type, PlayerKit kit) {
        String docKey = getKitTypeKey(type) + ".tiers." + kit.getId();

        addUpdate(docKey, kit.getCurrentTier());
    }

    public void addKit(GameRegistry.Type type, PlayerKit kit) {
        updateTier(type, kit);

        unlockedKits.put(type, kit);
    }

    public void setDefaultKit(GameRegistry.Type type, PlayerKit kit) {
        String docKey = getKitTypeKey(type) + ".equipped";
        addUpdate(docKey, kit.getId());

        unlockedKits.get(type).stream().filter(PlayerKit::isDefaultKit).forEach(old -> {
            old.setDefaultKit(false);
        });

        kit.setDefaultKit(true);
    }

    public PlayerKit getPlayerKit(GameRegistry.Type type, String kitId) {
        Collection<PlayerKit> kits = unlockedKits.get(type);

        if (kits == null) {
            return null;
        }

        return kits.stream()
                .filter(kit -> kit.getKit().getKitId().equals(kitId))
                .findAny()
                .orElse(null);
    }

    public boolean hasKit(GameRegistry.Type type, String kitId) {
        return getPlayerKit(type, kitId) != null;
    }

    private boolean hasKit(GameRegistry.Type type, PlayerKit kit) {
        return hasKit(type, kit.getKit().getKitId());
    }

    public PlayerKit getDefaultKit(GameRegistry.Type game) {
        return getPlayerKits(game).stream()
                .filter(PlayerKit::isDefaultKit)
                .findAny()
                .orElse(null);
    }

    public Collection<PlayerKit> getPlayerKits(GameRegistry.Type type) {
        return unlockedKits.get(type);
    }

    private int lowestTierFree(Kit kit) {
        int lowest = -1;
        for (int i = 0; i < kit.getTiers().length; i++) {
            if (kit.getTiers()[i].getPrice() == 0)
                lowest = i;
            else break;
        }
        return lowest;
    }

    private String getKitTypeKey(GameRegistry.Type type) {
        return PlayerKey.KITS + "." + type.name();
    }

    private boolean isManagerLoaded() {
        return KitManager.getInstance() != null && KitManager.getInstance().isLoaded();
    }
}
