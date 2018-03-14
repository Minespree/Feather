package net.minespree.feather.announcement;

import com.google.common.util.concurrent.FutureCallback;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.Scheduler;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementManager {

    private static final int TICKS_BETWEEN_ANNOUNCEMENTS = 20 * 60 * 5;
    @Getter
    private final static AnnouncementManager instance = new AnnouncementManager();

    private List<Announcement> announcements = new ArrayList<>();

    public void load() {
        Scheduler.run(() -> MongoManager.getInstance().getCollection("announcements"), new FutureCallback<MongoCollection<Document>>() {
            @Override
            public void onSuccess(MongoCollection<Document> documentMongoCollection) {
                FindIterable<Document> fi = documentMongoCollection.find();
                for (Document document : fi) {
                    load(document);
                }

                FeatherPlugin.get().getServer().getScheduler().runTaskTimer(FeatherPlugin.get(), new AnnouncementTask(),
                        TICKS_BETWEEN_ANNOUNCEMENTS, TICKS_BETWEEN_ANNOUNCEMENTS);
            }

            @Override
            public void onFailure(Throwable throwable) {
                try {
                    throw throwable;
                } catch (Throwable throwable1) {
                    throwable1.printStackTrace();
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void load(Document document) {
        BabelMessage message = Babel.translate(document.getString("message"));
        announcements.add(new Announcement(message));
    }

    public List<Announcement> getAnnouncements() {
        return announcements;
    }

}
