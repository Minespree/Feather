package net.minespree.feather.player.save;

import com.google.common.collect.Multimap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.BSONUtil;
import net.minespree.wizard.executors.BukkitAsyncExecutor;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MongoPlayerSaver implements PlayerSaver {
    private static final UpdateOptions SAVE_OPTIONS = new UpdateOptions().upsert(true);

    private final BukkitAsyncExecutor executor;
    private MongoCollection<Document> collection;

    public MongoPlayerSaver(MongoManager manager, FeatherPlugin plugin) {
        this.executor = plugin.getAsyncExecutor();
        this.collection = manager.getCollection("players");
    }

    @Override
    public CompletableFuture<Boolean> save(Saveable saveable) {
        LinkedHashMap<String, Object> queue = saveable.getSaveQueue();
        Multimap<String, Object> setQueue = saveable.getSaveSetQueue();
        LinkedHashMap<String, Object> operations = saveable.getSaveOperations();

        if (queue.isEmpty() && setQueue.isEmpty() && operations.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            Document updateDoc = new Document(new LinkedHashMap<>());

            Document setDoc = createSetDoc(queue);
            Document addToSetDoc = new Document(new LinkedHashMap<>());

            // TODO Check if this works properly
            setQueue.asMap().forEach(addToSetDoc::put);

            if (!addToSetDoc.isEmpty()) {
                addToSetDoc.forEach((key, val) -> {
                    if (val instanceof Collection) {
                        updateDoc.put("$addToSet", new Document(key, new Document("$each", val)));
                    } else {
                        updateDoc.put("$addToSet", val);
                    }
                });
            }

            if (!setDoc.isEmpty()) {
                updateDoc.put("$set", setDoc);
            }

            for (String key : operations.keySet()) {
                updateDoc.put(key, operations.get(key));
            }

            Bson identifier = BSONUtil.getIdentifier(saveable.getUuid());
            UpdateResult result = collection.updateOne(identifier, updateDoc, SAVE_OPTIONS);

            saveable.saved();

            return result.wasAcknowledged();
        }, executor);
    }

    private Document createSetDoc(Map<String, Object> queue) {
        return new Document(queue);
    }
}
