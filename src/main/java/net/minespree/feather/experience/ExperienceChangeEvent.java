package net.minespree.feather.experience;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minespree.feather.events.NetworkEvent;
import net.minespree.feather.player.NetworkPlayer;

@Getter
@AllArgsConstructor
public class ExperienceChangeEvent extends NetworkEvent {
    private NetworkPlayer player;

    private long oldExperience;
    private int oldLevel;

    public boolean hasLeveledUp() {
        return oldLevel < player.getLevel();
    }
}
