package net.minespree.feather.settings;

import net.minespree.feather.player.rank.Rank;
import net.minespree.feather.repository.RepoRegistry;
import net.minespree.feather.repository.types.BooleanType;
import net.minespree.feather.repository.types.Type;

import java.util.stream.Stream;

public class FeatherSettings {
    public static final Setting VISIBILITY = new Setting("visibility", "setting_player_visibility", "setting_player_visibility_lore", Visibility.TYPE, Visibility.ALL, 5000);
    public static final Setting MESSAGES = new Setting("messages", "setting_message", "setting_message_lore", MessageVisibility.TYPE, MessageVisibility.ANYONE);
    private static final Type DEFAULT_TYPE = new BooleanType();
    public static final Setting FRIEND_REQUESTS = new Setting("friendRequests", "setting_friend_requests", "setting_friend_requests_lore", DEFAULT_TYPE, true);
    public static final Setting PARTY_REQUESTS = new Setting("partyRequests", "setting_party_requests", "setting_party_requests_lore", DEFAULT_TYPE, true);

    public static final Setting NEXT_GAME = new Setting("autoNextGame", "setting_next_game", "setting_next_game_lore", DEFAULT_TYPE, false);
    public static final Setting COLOR_BLIND = new Setting("colorBlind", "setting_colour_blind", "setting_colour_blind_lore", DEFAULT_TYPE, false);
    public static final Setting UPDATE_LOG = new Setting("updateLog", "setting_update_log", "setting_update_log_lore", DEFAULT_TYPE, true);

    public static final Setting HUB_SPEED = new Setting("hubSpeed", "setting_hub_speed", "setting_hub_speed_lore", Rank.IRON, DEFAULT_TYPE, false);
    public static final Setting HUB_FLY = new Setting("hubFly", "setting_hub_fly", "setting_hub_fly_lore", Rank.DIAMOND, DEFAULT_TYPE, false);

    // Only used on staff members
    public static final Setting STAFF_CHAT = new Setting("staffChat", "setting_staffchat", "setting_staffchat_lore", Rank.HELPER, DEFAULT_TYPE, true);
    public static final Setting ANNOUNCEMENTS = new Setting("announcements", "setting_announcements", "setting_announcements_lore", DEFAULT_TYPE, true);

    public static void register(RepoRegistry<Setting> registry) {
        Stream.of(
                VISIBILITY,
                MESSAGES,
                FRIEND_REQUESTS,
                PARTY_REQUESTS,
                NEXT_GAME,
                COLOR_BLIND,
                UPDATE_LOG,
                HUB_SPEED,
                HUB_FLY,
                STAFF_CHAT,
                ANNOUNCEMENTS
        ).forEach(registry::register);
    }

}
