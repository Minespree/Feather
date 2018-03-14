package net.minespree.feather.crates;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minespree.feather.util.BSONUtil;
import org.bson.Document;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Crate {
    private CrateType type;
    /**
     * Manually bought from the online store
     */
    private boolean bought;
    private LocalDateTime obtained;
    private boolean superCrate;

    /**
     * Seasonal/Event crate constructor
     *
     * @throws IllegalArgumentException if the {@link CrateType} isn't {@link CrateType#isEvent()} or {@link CrateType#isSeasonal()}
     */
    public Crate(CrateType type, boolean bought, LocalDateTime obtained) {
        this(type, bought, obtained, false);
        // "Pre"conditions
        Preconditions.checkArgument(type.isEvent() || type.isSeasonal());
    }

    public boolean expires() {
        return !bought;
    }

    public boolean hasExpired() {
        if (!expires() || !type.isSeasonal()) {
            return false;
        }

        SeasonalEvent event = type.getSeasonalEvent();

        return !event.isActive() && event.inTimeout(type.getAfterExpiry());
    }

    public Document toDocument() {
        Document document = new Document();

        document.put("bought", bought);
        document.put("type", type.getId());
        document.put("obtained", BSONUtil.toMongoDate(obtained));
        document.put("super", superCrate);

        return document;
    }
}
