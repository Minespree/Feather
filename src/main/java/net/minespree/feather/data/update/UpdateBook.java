package net.minespree.feather.data.update;

import com.mongodb.client.MongoCollection;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.ItemUtil;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UpdateBook {

    private int version;
    private ItemStack stack;

    @SuppressWarnings("all")
    public UpdateBook() {
        MongoCollection<Document> collection = MongoManager.getInstance().getCollection("update_book");
        CompletableFuture.supplyAsync(new Supplier<Document>() {
            @Override
            public Document get() {
                return collection.find().sort(new Document("updateVersion", -1)).first();
            }
        }).whenCompleteAsync(new BiConsumer<Document, Throwable>() {
            @Override
            public void accept(Document document, Throwable throwable) {
                if (throwable != null) {
                    throwable.printStackTrace();
                    version = -1;
                    return;
                }

                if (document == null) return;

                version = document.getInteger("updateVersion");
                List<String> pages = ((List<Object>) document.get("pages")).stream().map(o -> (String) o).collect(Collectors.toList());

                List<BaseComponent> bookPages = new ArrayList<>();
                for (String page : pages) {
                    bookPages.add(new TextComponent(ChatColor.translateAlternateColorCodes('&', page)));
                }
                stack = ItemUtil.createBook("UpdateBook", "Minespree", bookPages);
                System.out.println("Loaded Update Book v" + version + " with " + pages.size() + " pages.");
            }
        }).thenApply(s -> null);
    }

    public int getVersion() {
        return version;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return "UpdateBook{" +
                "version=" + version +
                ", stack=" + stack +
                '}';
    }
}
