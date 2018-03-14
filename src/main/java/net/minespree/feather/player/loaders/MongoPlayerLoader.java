package net.minespree.feather.player.loaders;

import com.mongodb.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.util.BSONUtil;
import net.minespree.wizard.executors.BukkitAsyncExecutor;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class MongoPlayerLoader implements PlayerLoader {
    private final BukkitAsyncExecutor executor;
    private final MongoManager manager;

    public MongoPlayerLoader(MongoManager manager, FeatherPlugin plugin) {
        this.executor = plugin.getAsyncExecutor();
        this.manager = manager;
    }

    @Override
    public <T extends NetworkPlayer> CompletableFuture<T> loadPlayer(UUID uuid, Supplier<T> supplier, boolean bootstrap) {
        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> collection = manager.getCollection("players");
            Document playerDocument = collection.find(BSONUtil.getIdentifier(uuid)).first();

            T player = supplier.get();

            if (playerDocument == null) {
                // Load up default values, probably first join
                playerDocument = new Document();
            }

            player.load(playerDocument, bootstrap);

            return player;
        }, executor);
    }
}
