package net.minespree.feather.achievements.impl;

import net.minespree.feather.achievements.Achievement;
import net.minespree.feather.achievements.AchievementBuilder;
import net.minespree.feather.achievements.AchievementReward;
import net.minespree.feather.achievements.RewardType;
import net.minespree.feather.repository.RepoRegistry;
import net.minespree.feather.repository.types.EnumType;

import java.util.stream.Stream;

public class FeatherAchievements {
    public static final Achievement FIRST_JOIN = new AchievementBuilder()
            .id("firstJoin")
            .name("ac_first_join")
            .description("ac_first_join_desc")
            .reward(new AchievementReward(RewardType.COINS, 50))
            .reward(new AchievementReward(RewardType.EXPERIENCE, 100))
            .get();

    public static final Achievement PINEAPPLE = new AchievementBuilder()
            .id("pineapple")
            .name("ac_pineapple")
            .description("ac_pineapple_desc")
            .reward(new AchievementReward(RewardType.COINS, 250))
            .get();

    public static final Achievement SAME_LOBBY_STAFF = new AchievementBuilder()
            .id("sameLobbyStaff")
            .name("ac_same_lobby_staff")
            .description("ac_same_lobby_staff_desc")
            .get();

    public static final Achievement PLAY_TIME = new AchievementBuilder()
            .id("playTime")
            .name("ac_play_time")
            .description("ac_play_time_desc")
            .type(new EnumType<>("PlayTime", PlayTime.class))
            .get();

    public static final Achievement PLAY_ALL_GAMES = new AchievementBuilder()
            .id("playAllGames")
            .name("ac_play_all_games")
            .description("ac_play_all_games_desc")
            .get();

    public static final Achievement WIN_ALL_GAMES = new AchievementBuilder()
            .id("winAllGames")
            .name("ac_win_all_games")
            .description("ac_win_all_games_desc")
            .get();

    public static final Achievement KOTL = new AchievementBuilder()
            .id("kotl")
            .name("ac_kotl")
            .description("ac_kotl_desc")
            .get();

    public static final Achievement KING_OF_KINGS = new AchievementBuilder()
            .id("kingOfKings")
            .name("ac_king_of_kings")
            .description("ac_king_of_kings_desc")
            .get();

    public static final Achievement ADD_FRIEND = new AchievementBuilder()
            .id("addFriend")
            .name("ac_add_friend")
            .description("ac_add_friend_desc")
            .get();

    public static final Achievement CREATE_PARTY = new AchievementBuilder()
            .id("createParty")
            .name("ac_create_party")
            .description("ac_create_party")
            .get();

    public static final Achievement SUPPORT_SERVER = new AchievementBuilder()
            .id("supportServer")
            .name("ac_support_server")
            .description("ac_support_server_desc")
            .get();

    public static void register(RepoRegistry<Achievement> registry) {
        Stream.of(
                FIRST_JOIN,
                PINEAPPLE,
                SAME_LOBBY_STAFF,
                PLAY_TIME,
                PLAY_ALL_GAMES,
                WIN_ALL_GAMES,
                KOTL,
                KING_OF_KINGS,
                ADD_FRIEND,
                CREATE_PARTY,
                SUPPORT_SERVER
        ).forEach(registry::register);
    }
}
