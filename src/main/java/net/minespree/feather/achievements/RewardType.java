package net.minespree.feather.achievements;

import lombok.AllArgsConstructor;
import net.minespree.babel.Babel;
import net.minespree.feather.player.NetworkPlayer;
import org.bukkit.ChatColor;

import java.text.NumberFormat;

@AllArgsConstructor
public enum RewardType {
    COINS("coin", "coins", ChatColor.GOLD) {
        @Override
        public void apply(NetworkPlayer player, int value) {
            player.addCoins(value);
        }
    },
    EXPERIENCE("experience", "experience", ChatColor.BLUE) {
        @Override
        public void apply(NetworkPlayer player, int value) {
            player.addExperience(value);
        }
    };

    private static final String BABEL_REWARD_MSG = "achievement_earned_x_unit";

    private String nameSingular;
    private String namePlural;
    private ChatColor color;

    public abstract void apply(NetworkPlayer player, int value);

    public void sendRewardMessage(NetworkPlayer player, int value) {
        String units = value != 1 ? namePlural : nameSingular;

        String formatted = NumberFormat.getInstance().format(value);
        String message = Babel.translate(BABEL_REWARD_MSG).toString(color, formatted, units);

        player.sendMessage(message);
    }
}
