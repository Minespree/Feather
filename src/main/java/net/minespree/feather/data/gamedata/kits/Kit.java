package net.minespree.feather.data.gamedata.kits;

import lombok.Data;
import lombok.NonNull;
import net.minespree.wizard.gui.PerPlayerInventoryGUI;

/**
 *
 */
@Data
public class Kit {

    /**
     *
     */
    @NonNull
    private final String kitId;
    /**
     *
     */
    @NonNull
    private final Tier[] tiers;
    /**
     *
     */
    @NonNull
    private final boolean defaultKit;
    /**
     *
     */
    @NonNull
    private final PerPlayerInventoryGUI tierMenu;

}
