package net.minespree.feather.player.punishments;

import com.mongodb.client.MongoCollection;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.Scheduler;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;

import java.util.UUID;

public class Punishment {
    private PunishmentType type;
    private UUID source;
    private UUID target;
    private String reason;
    private long timestamp;
    private long until;

    private String punishmentId;
    private int score = 1;

    public Punishment(PunishmentType type, UUID source, UUID target, String reason, long timestamp, long until) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.reason = reason;
        this.timestamp = timestamp;
        this.until = until;
    }

    public PunishmentType getType() {
        return type;
    }

    public UUID getSource() {
        return source;
    }

    public UUID getTarget() {
        return target;
    }

    public String getReason() {
        return reason;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getUntil() {
        return until;
    }

    public void setUntil(long until) {
        this.until = until;
    }

    public boolean hasExpired() {
        return until != -1 && until <= System.currentTimeMillis();
    }

    public String getId() {
        return punishmentId;
    }

    public void save() {
        Scheduler.runAsync(() -> {
            MongoCollection<Document> punishCollection = MongoManager.getInstance().getCollection("punishments");
            Document doc = new Document("target", target.toString())
                    .append("source", source.toString())
                    .append("type", type.toString())
                    .append("until", until)
                    .append("timestamp", timestamp)
                    .append("reason", reason)
                    .append("undone", false)
                    .append("appealCode", RandomStringUtils.randomAlphanumeric(6).toUpperCase())
                    .append("punishmentId", punishmentId = RandomStringUtils.randomAlphabetic(6))
                    .append("score", score);

            punishCollection.insertOne(doc);
        });
    }
}
