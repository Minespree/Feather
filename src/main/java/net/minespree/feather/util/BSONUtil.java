package net.minespree.feather.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BSONUtil {
    public static Stream<String> stringListToStream(Document document, String key) {
        Preconditions.checkNotNull(document);
        Preconditions.checkNotNull(key);

        List<Object> list = (List<Object>) document.get(key);

        if (list == null) {
            return null;
        }

        return list.stream().filter(Objects::nonNull).map(String::valueOf);
    }

    public static Set<String> stringListToSet(Document document, String key) {
        Stream<String> stream = stringListToStream(document, key);

        if (stream == null) {
            return Sets.newHashSet();
        }

        return stream.collect(Collectors.toSet());
    }

    public static Set<UUID> uuidListToSet(Document document, String key) {
        Stream<String> stream = stringListToStream(document, key);

        if (stream == null) {
            return Sets.newHashSet();
        }

        return stream.map(UUID::fromString).collect(Collectors.toSet());
    }

    public static Document getSubDoc(Document parent, String key) {
        return (Document) parent.getOrDefault(key, new Document());
    }

    public static List<Document> getSubDocList(Document parent, String key) {
        return (List<Document>) parent.getOrDefault(key, Collections.emptyList());
    }

    public static String wrapEmptyString(Document document, String key) {
        String value = document.getString(key);

        return value == null ? null : (value.isEmpty() ? null : value);
    }

    public static Bson getIdentifier(UUID uuid) {
        return Filters.eq("_id", uuid.toString());
    }

    public static Date toMongoDate(LocalDate date) {
        Instant instant = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();

        return Date.from(instant);
    }

    public static Date toMongoDate(LocalDateTime time) {
        Instant instant = time.atZone(ZoneId.systemDefault()).toInstant();

        return Date.from(instant);
    }
}
