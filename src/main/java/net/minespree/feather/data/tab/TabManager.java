package net.minespree.feather.data.tab;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import net.minespree.feather.FeatherPlugin;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.Scheduler;
import net.minespree.wizard.util.MessageUtil;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabManager {

    @Getter
    private final static TabManager instance = new TabManager();

    private TabType header, footer;

    public void load() {
        ListenableFuture<Document> future = Scheduler.getPublicExecutor().submit(() ->
                MongoManager.getInstance().getCollection("nexus").find(Filters.eq("_id", "tabmanager")).first());
        Futures.addCallback(future, new FutureCallback<Document>() {
            @Override
            public void onSuccess(Document document) {
                header = from((Document) document.get("header"));
                footer = from((Document) document.get("footer"));

                Bukkit.getScheduler().runTaskTimer(FeatherPlugin.get(), () -> {
                    header.tick();
                    footer.tick();
                    Bukkit.getOnlinePlayers().forEach(player -> TabManager.getInstance().set(player));
                }, 1L, 1L);
            }

            @Override
            public void onFailure(Throwable throwable) {
                throw new RuntimeException("Failed to load tab manager");
            }
        });
    }

    public void set(Player player) {
        MessageUtil.sendTabHeaderFooter(player, header.getCurrent().getStr(), footer.getCurrent().getStr());
    }

    @SuppressWarnings("unchecked")
    public TabType from(Document document) {
        Map<Integer, TabData> animatedStrings = new HashMap<>();
        for (String id : document.keySet()) {
            Document doc = (Document) document.get(id);
            int i = Integer.parseInt(id);
            int interval = doc.getInteger("interval");
            List<String> strings = (List<String>) doc.get("strings");
            animatedStrings.put(i, new TabData(interval, strings));
        }
        return new TabType(animatedStrings);
    }

    private class TabType {

        private Map<Integer, TabData> animatedStrings;

        private int i = 0, tick = 0;

        TabType(Map<Integer, TabData> animatedStrings) {
            this.animatedStrings = animatedStrings;
        }

        public void tick() {
            if (i >= animatedStrings.size() - 1)
                i = 0;
            if (tick >= animatedStrings.get(i).getInterval()) {
                i++;
                tick = 0;
                return;
            }
            tick++;
        }

        public TabData getCurrent() {
            return animatedStrings.getOrDefault(i, animatedStrings.get(0));
        }

    }

    private class TabData {

        @Getter
        private int interval;
        @Getter
        private String str;

        TabData(int interval, List<String> strings) {
            this.interval = interval;

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < strings.size(); i++) {
                builder.append(ChatColor.translateAlternateColorCodes('&', strings.get(i)));
                if (i < strings.size() - 1)
                    builder.append("\n");
            }
            this.str = builder.toString();
        }

    }

}
