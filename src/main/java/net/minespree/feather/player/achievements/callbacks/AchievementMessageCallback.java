package net.minespree.feather.player.achievements.callbacks;

import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.achievements.PlayerAchievementCallback;
import net.minespree.wizard.util.Chat;

public class AchievementMessageCallback extends PlayerAchievementCallback {
    private static final BabelMessage ACHIEVEMENT_AWARDED_MSG = Babel.translate("achievement-awarded");

    @Override
    public void notifyChange(NetworkPlayer player, Achievement achievement, Object oldValue, Object newValue) {
        player.sendMessage(" ");
        player.sendMessage(Chat.center(Chat.GREEN + ACHIEVEMENT_AWARDED_MSG.toString(player.getPlayer())));
        player.sendMessage(" ");
        player.sendMessage(Chat.center(Chat.GOLD + Chat.BOLD + getAchievementName(achievement, player)));
        player.sendMessage(Chat.center(Chat.GRAY + getAchievementDesc(achievement, player)));
        player.sendMessage(" ");
    }

    private String getAchievementName(Achievement achievement, NetworkPlayer player) {
        BabelMessage message = Babel.translate(achievement.getBabel());

        return message.toString(player.getPlayer());
    }

    private String getAchievementDesc(Achievement achievement, NetworkPlayer player) {
        BabelMessage message = Babel.translate(achievement.getBabelDescription());

        return message.toString(player.getPlayer());
    }
}
