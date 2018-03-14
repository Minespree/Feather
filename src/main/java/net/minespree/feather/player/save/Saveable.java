package net.minespree.feather.player.save;

import com.google.common.collect.Multimap;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.LinkedHashMap;
import java.util.UUID;

public interface Saveable {
    UUID getUuid();

    long getLastSave();

    default boolean shouldSave(long threshold) {
        return (!getSaveQueue().isEmpty() || !getSaveSetQueue().isEmpty() || !getSaveOperations().isEmpty()) && System.currentTimeMillis() - getLastSave() >= threshold;
    }

    void saved();

    void addUpdate(String key, Object value);

    LinkedHashMap<String, Object> getSaveQueue();

    Multimap<String, Object> getSaveSetQueue();

    void addSetUpdate(String key, Object valueToAdd);

    LinkedHashMap<String, Object> getSaveOperations();

    /**
     * Adds a primitive MongoDB update operation to the save queue.
     * It will be added to an update {@link Document} which will
     * get sent through {@link com.mongodb.client.MongoCollection#updateOne(Bson, Bson)}
     *
     * @throws IllegalArgumentException if the key is $set or $addToSet. You should
     *                                  use default update methods instead.
     */
    void addSaveOperation(String key, Object document);
}
