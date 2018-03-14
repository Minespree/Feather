package net.minespree.feather.data.chat;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.LoggingUtils;
import net.minespree.feather.util.Scheduler;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;

public class ChatManager {

    private static final int MAXIMUM_IN_BATCH = 50;
    private final Queue<ChatData> messageQueue = new ConcurrentLinkedDeque<>();

    public ChatManager() {
        FeatherPlugin.get().getServer().getScheduler().runTaskTimerAsynchronously(FeatherPlugin.get(), () -> {
            List<UpdateOneModel<Document>> models = new ArrayList<>();
            ChatData data;
            while ((data = messageQueue.poll()) != null) {
                Document added = new Document("message", data.getMessage()).
                        append("timestamp", data.getTimeStamp()).
                        append("server", data.getServer()).
                        append("rawText", data.getRawText());

                if (data.getTarget() != null)
                    added.append("target", data.getTarget().toString());

                models.add(new UpdateOneModel<>(
                        Filters.eq("_id", data.getSender().getUniqueId().toString()),
                        new Document("$addToSet", new Document(data.getType().name(), added)),
                        new UpdateOptions().upsert(true)
                ));

                if (models.size() >= MAXIMUM_IN_BATCH) {
                    break;
                }
            }

            Scheduler.runAsync(() -> {
                MongoCollection<Document> messages = MongoManager.getInstance().getCollection("messages");
                BulkWriteResult result = messages.bulkWrite(models, new BulkWriteOptions().ordered(true));

                if (result.getModifiedCount() <= 0) {
                    LoggingUtils.log(Level.SEVERE, "Failed insert for {0} messages: {1}", models.size(), result);
                }
            });
        }, 0L, 200L);
    }

    public void insertMessage(ChatData message) {
        messageQueue.add(message);
    }

    public enum ChatType {
        PUBLIC,
        PRIVATE
    }

    @Data
    @AllArgsConstructor
    public static class ChatData {
        private Player sender;
        private String message;
        private long timeStamp;
        private String server;
        private String rawText;
        private ChatType type;
        private UUID target;
    }

}
