package net.minespree.feather.util;

import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.player.PlayerKey;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UUIDNameKeypair {

    private UUID uuid;
    private String name;

    public UUIDNameKeypair(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static UUIDNameKeypair generate(String name) {
        ProfileRepository repository = new HttpProfileRepository("minecraft");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return new UUIDNameKeypair(p.getUniqueId(), p.getName());
            }
        }

        Profile[] profiles = repository.findProfilesByNames(name);
        if (profiles.length == 1) {
            return new UUIDNameKeypair(profiles[0].getUUID(), profiles[0].getName());
        } else {
            return null;
        }
    }

    public static void generate(UUID uuid, Callback<UUIDNameKeypair> callback) {
        Scheduler.runAsync(() -> {
            MongoCollection<Document> players = MongoManager.getInstance().getCollection("players");
            Document find = players.find(Filters.eq("_id", uuid.toString())).first();
            if (find != null) {
                callback.call(new UUIDNameKeypair(uuid, find.getString(PlayerKey.LAST_NAME)));
            } else {
                callback.call(null);
            }
        });
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
