package net.minespree.feather.data.gamedata.kits;

import lombok.Data;
import lombok.NonNull;

@Data
public class PlayerKit {

    /**
     *
     */
    @NonNull
    private final Kit kit;
    /**
     *
     */
    @NonNull
    private int currentTier;
    /**
     *
     */
    @NonNull
    private Tier tier;
    /**
     *
     */
    @NonNull
    private boolean defaultKit;

    public String getId() {
        return kit.getKitId();
    }

}
