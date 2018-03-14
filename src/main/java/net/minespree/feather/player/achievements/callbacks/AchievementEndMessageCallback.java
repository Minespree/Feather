package net.minespree.feather.player.achievements.callbacks;

import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.achievements.PlayerAchievementCallback;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AchievementEndMessageCallback extends PlayerAchievementCallback {
    @Override
    public void notifyChange(NetworkPlayer player, Achievement achievement, Object oldValue, Object newValue) {
        player.sendMessage(" ");

        Player bukkit = player.getPlayer();
        bukkit.playSound(bukkit.getLocation(), Sound.LEVEL_UP, 1, 1.5F);
    }
}
