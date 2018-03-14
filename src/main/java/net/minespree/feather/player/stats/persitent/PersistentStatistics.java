package net.minespree.feather.player.stats.persitent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.BsonGenerics;
import net.minespree.feather.util.Callback;
import net.minespree.feather.util.LoggingUtils;
import net.minespree.feather.util.Scheduler;
import org.bson.Document;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

// This class is very soon and very fast going to become legacy code. Do NOT touch it unless instructed to.
public class PersistentStatistics {

    public static final BiFunction<String, String, String> DOT_NOTATION_FUNCTION = (type, s) -> "statistics." + type + "." + s + ".";
    private final UUID owner;

    private Multimap<GameRegistry.Type, PersistableStatistic> peristableStatistics = ArrayListMultimap.create();

    public PersistentStatistics(UUID owner) {
        this.owner = owner;
    }

    public static <T> T getFrom(Document parent, String dotNotation, Class<T> clazz) {
        Map<String, Object> cur = parent;
        int dot;
        while ((dot = dotNotation.indexOf('.')) > 0) {
            String part = dotNotation.substring(0, dot);
            Object i = cur.get(part);
            if (!(i instanceof Map)) {
                return null;
            }
            cur = (Map<String, Object>) i;
            dotNotation = dotNotation.substring(dot + 1);
        }
        Object object = cur.get(dotNotation);
        return clazz.cast(object);
    }

    public static String constructKey(GameRegistry.Type type, PersistableData dataType, String key) {
        return DOT_NOTATION_FUNCTION.apply(type.name(), dataType.name().toLowerCase()) + key;
    }

    public PersistableStatistic getPersistableStatistic(GameRegistry.Type type, Class<? extends PersistableStatistic> clazz) {
        try {
            PersistableStatistic instance = (PersistableStatistic) clazz.getConstructors()[0].newInstance(type.name());
            peristableStatistics.put(type, instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public PersistableIntegerStatistic getIntegerStatistics(GameRegistry.Type type) {
        PersistableIntegerStatistic statistic = new PersistableIntegerStatistic(type.name());
        peristableStatistics.put(type, statistic);
        return statistic;
    }

    public PersistableFloatStatistic getFloatStatistics(GameRegistry.Type type) {
        PersistableFloatStatistic statistic = new PersistableFloatStatistic(type.name());
        peristableStatistics.put(type, statistic);
        return statistic;
    }

    public PersistableLongStatistic getLongStatistics(GameRegistry.Type type) {
        PersistableLongStatistic statistic = new PersistableLongStatistic(type.name());
        peristableStatistics.put(type, statistic);
        return statistic;
    }

    public PersistableDoubleStatistic getDoubleStatistics(GameRegistry.Type type) {
        PersistableDoubleStatistic statistic = new PersistableDoubleStatistic(type.name());
        peristableStatistics.put(type, statistic);
        return statistic;
    }

    public PersistableBooleanStatistic getBooleanStatistics(GameRegistry.Type type) {
        PersistableBooleanStatistic statistic = new PersistableBooleanStatistic(type.name());
        peristableStatistics.put(type, statistic);
        return statistic;
    }

    public PersistableLocationStatistic getLocationStatistics(GameRegistry.Type type) {
        PersistableLocationStatistic statistic = new PersistableLocationStatistic(type.name());
        peristableStatistics.put(type, statistic);
        return statistic;
    }

    public PersistableStringStatistic getStringStatistics(GameRegistry.Type type) {
        PersistableStringStatistic statistic = new PersistableStringStatistic(type.name());
        peristableStatistics.put(type, statistic);
        return statistic;
    }

    public void getValue(String key, Callback<Object> callback) {
        if (!key.contains(".")) throw new UnsupportedOperationException("does not utilize dot notation");

        ListenableFuture<Document> future = Scheduler.getPublicExecutor().submit(() -> MongoManager.getInstance().getCollection("players").find(Filters.eq("_id", owner.toString())).first());
        class SingleTimeReference {
            String key;
        }
        SingleTimeReference reference = new SingleTimeReference();
        reference.key = key;
        Futures.addCallback(future, new FutureCallback<Document>() {
            @Override
            public void onSuccess(@Nullable Document document) {
                Preconditions.checkNotNull(document);
                Map<String, Object> cur = document;
                int dot;
                while ((dot = reference.key.indexOf('.')) > 0) {
                    String part = reference.key.substring(0, dot);
                    Object i = cur.get(part);
                    if (!(i instanceof Map)) {
                        callback.call(null);
                        break;
                    }
                    cur = (Map<String, Object>) i;
                    reference.key = reference.key.substring(dot + 1);
                }
                Object object = cur.get(reference.key);
                callback.call(object);
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.call(null);
            }
        });
    }

    @Deprecated
    public Object _getValue(String key) {
        LoggingUtils.log(Level.WARNING, "Usage of PersistentStatistics#_getValue may be removed in future updates, please refrain from using it.");

        class SingleTimeReference {
            private Object value;
        }
        SingleTimeReference reference = new SingleTimeReference();
        getValue(key, document -> reference.value = document);

        return reference.value;
    }

    public void persist() {
        MongoCollection<Document> playersCollection = MongoManager.getInstance().getCollection("players");
        List<WriteModel<? extends Document>> writeModels = Lists.newArrayList();

        for (GameRegistry.Type type : peristableStatistics.keySet()) {
            List<PersistableStatistic> statistics = new ArrayList<>(peristableStatistics.get(type));

            for (PersistableStatistic statistic : statistics) {
                Document statParent = new Document();
                statistic.save(statParent);

                UpdateOneModel<Document> model = new UpdateOneModel<>(Filters.eq("_id", owner.toString()), statParent);
                model.getOptions().upsert(true);
                writeModels.add(model);
            }
        }

        Scheduler.runAsync(() -> playersCollection.bulkWrite(writeModels));
        peristableStatistics.clear();
    }

    public enum PersistableData {
        INTEGERS,
        DOUBLES,
        FLOATS,
        BOOLEANS,
        LOCATIONS
    }

    private enum IntegerAction {
        INCREMENT("$inc"),
        SET("$set");

        String action;

        IntegerAction(String action) {
            this.action = action;
        }
    }

    private enum PossibleMultiStore {
        SET("$set"),
        PUSH("$push"),
        PULL("$pull"),
        EACH("$each"),
        ADD_TO_SET("$addToSet");

        String action;

        PossibleMultiStore(String action) {
            this.action = action;
        }
    }

    private class ActionWrapper<T extends Number> {
        public String key;
        public T update;

        ActionWrapper(String key, T update) {
            this.key = key;
            this.update = update;
        }

        public T getUpdate() {
            return update;
        }

        public String getKey() {
            return key;
        }
    }

    public abstract class PersistableStatistic<T> {

        protected abstract String getObjectName();

        protected abstract void save(Document document);
    }

    public class PersistableIntegerStatistic extends PersistableStatistic<Integer> {

        private final String type;
        private Multimap<IntegerAction, ActionWrapper> documentSet = ArrayListMultimap.create();

        PersistableIntegerStatistic(String type) {
            this.type = type;
        }

        public PersistableIntegerStatistic increment(String key, int value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        public PersistableIntegerStatistic decrease(String key, int value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, -value));
            return this;
        }

        public PersistableIntegerStatistic set(String key, int value) {
            Preconditions.checkNotNull(key);
            documentSet.put(IntegerAction.SET, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        @Override
        protected String getObjectName() {
            return DOT_NOTATION_FUNCTION.apply(type, "integers");
        }

        @Override
        protected void save(Document document) {
            Map<String, Map<String, Integer>> collected = documentSet.asMap().entrySet().stream().
                    collect(Collectors.toMap(e -> e.getKey().action, e -> e.getValue().stream().collect(Collectors.toMap(a -> a.key, a -> a.update.intValue()))));

            document.putAll(collected);
        }

    }

    public class PersistableDoubleStatistic extends PersistableStatistic<Double> {

        private final String type;
        private Multimap<IntegerAction, ActionWrapper> documentSet = ArrayListMultimap.create();

        PersistableDoubleStatistic(String type) {
            this.type = type;
        }

        public PersistableDoubleStatistic increment(String key, double value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        public PersistableDoubleStatistic decrease(String key, double value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, -value));
            return this;
        }

        public PersistableDoubleStatistic set(String key, double value) {
            Preconditions.checkNotNull(key);
            documentSet.put(IntegerAction.SET, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        @Override
        protected String getObjectName() {
            return DOT_NOTATION_FUNCTION.apply(type, "doubles");
        }

        @Override
        protected void save(Document document) {
            Map<String, Map<String, Double>> collected = documentSet.asMap().entrySet().stream().
                    collect(Collectors.toMap(e -> e.getKey().action, e -> e.getValue().stream().collect(Collectors.toMap(a -> a.key, a -> a.update.doubleValue()))));

            document.putAll(collected);
        }

    }

    public class PersistableFloatStatistic extends PersistableStatistic<Float> {

        private final String type;
        private Multimap<IntegerAction, ActionWrapper> documentSet = ArrayListMultimap.create();

        PersistableFloatStatistic(String type) {
            this.type = type;
        }

        public PersistableFloatStatistic increment(String key, float value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        public PersistableFloatStatistic decrease(String key, float value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, -value));
            return this;
        }

        public PersistableFloatStatistic set(String key, float value) {
            Preconditions.checkNotNull(key);
            documentSet.put(IntegerAction.SET, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        @Override
        protected String getObjectName() {
            return DOT_NOTATION_FUNCTION.apply(type, "floats");
        }

        @Override
        protected void save(Document document) {
            Map<String, Map<String, Float>> collected = documentSet.asMap().entrySet().stream().
                    collect(Collectors.toMap(e -> e.getKey().action, e -> e.getValue().stream().collect(Collectors.toMap(a -> a.key, a -> a.update.floatValue()))));

            document.putAll(collected);
        }

    }

    public class PersistableLongStatistic extends PersistableStatistic<Long> {

        private final String type;
        private Multimap<IntegerAction, ActionWrapper> documentSet = ArrayListMultimap.create();

        PersistableLongStatistic(String type) {
            this.type = type;
        }

        public PersistableLongStatistic increment(String key, long value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        public PersistableLongStatistic decrease(String key, long value) {
            Preconditions.checkNotNull(key);
            if (value < 0) throw new UnsupportedOperationException("amount is negative");
            documentSet.put(IntegerAction.INCREMENT, new ActionWrapper<>(getObjectName() + key, -value));
            return this;
        }

        public PersistableLongStatistic set(String key, long value) {
            Preconditions.checkNotNull(key);
            documentSet.put(IntegerAction.SET, new ActionWrapper<>(getObjectName() + key, value));
            return this;
        }

        @Override
        protected String getObjectName() {
            return DOT_NOTATION_FUNCTION.apply(type, "longs");
        }

        @Override
        protected void save(Document document) {
            Map<String, Map<String, Long>> collected = documentSet.asMap().entrySet().stream().
                    collect(Collectors.toMap(e -> e.getKey().action, e -> e.getValue().stream().collect(Collectors.toMap(a -> a.key, a -> a.update.longValue()))));

            document.putAll(collected);
        }

    }

    public class PersistableBooleanStatistic extends PersistableStatistic<Boolean> {

        private final String type;
        private Set<Document> documentSet = Sets.newHashSet();

        PersistableBooleanStatistic(String type) {
            this.type = type;
        }

        public PersistableBooleanStatistic set(String key, boolean value) {
            Preconditions.checkNotNull(key);
            documentSet.add(new Document(getObjectName() + key, value));
            return this;
        }

        @Override
        protected String getObjectName() {
            return DOT_NOTATION_FUNCTION.apply(type, "booleans");
        }

        @Override
        protected void save(Document document) {
            documentSet.forEach(doc -> document.put("$set", doc));
        }
    }

    public class PersistableStringStatistic extends PersistableStatistic<String> {

        private final String type;
        private Map<Document, PossibleMultiStore> documentSet = Maps.newHashMap();

        PersistableStringStatistic(String type) {
            this.type = type;
        }

        public PersistableStringStatistic set(String key, @Nullable String value) {
            Preconditions.checkNotNull(key);
            documentSet.put(new Document(getObjectName() + key, value), PossibleMultiStore.SET);
            return this;
        }

        public PersistableStringStatistic push(String arrayKey, String value) {
            Preconditions.checkNotNull(arrayKey);
            Preconditions.checkNotNull(value);
            documentSet.put(new Document(getObjectName() + arrayKey, value), PossibleMultiStore.PUSH);
            return this;
        }

        public PersistableStringStatistic pull(String arrayKey, String value) {
            Preconditions.checkNotNull(arrayKey);
            Preconditions.checkNotNull(value);
            documentSet.put(new Document(getObjectName() + arrayKey, value), PossibleMultiStore.PULL);
            return this;
        }

        public PersistableStringStatistic addToSet(String arrayKey, String value) {
            Preconditions.checkNotNull(arrayKey);
            Preconditions.checkNotNull(value);
            documentSet.put(new Document(getObjectName() + arrayKey, value), PossibleMultiStore.ADD_TO_SET);
            return this;
        }

        @Override
        protected String getObjectName() {
            return DOT_NOTATION_FUNCTION.apply(type, "strings");
        }

        @Override
        protected void save(Document document) {
            documentSet.forEach((doc, store) -> document.append(store.action, doc));
        }
    }

    public class PersistableLocationStatistic extends PersistableStatistic<Location> {

        private final String type;
        private Map<Document, PossibleMultiStore> documentSet = Maps.newHashMap();

        PersistableLocationStatistic(String type) {
            this.type = type;
        }

        public PersistableLocationStatistic set(String key, @Nullable Location value) {
            Preconditions.checkNotNull(key);
            documentSet.put(new Document(getObjectName() + key, BsonGenerics.LOCATION_TO_DOCUMENT.apply(value)), PossibleMultiStore.SET);
            return this;
        }

        public PersistableLocationStatistic push(String arrayKey, Location value) {
            Preconditions.checkNotNull(arrayKey);
            Preconditions.checkNotNull(value);
            documentSet.put(new Document(getObjectName() + arrayKey, BsonGenerics.LOCATION_TO_DOCUMENT.apply(value)), PossibleMultiStore.PUSH);
            return this;
        }

        @Override
        protected String getObjectName() {
            return DOT_NOTATION_FUNCTION.apply(type, "locations");
        }

        @Override
        protected void save(Document document) {
            documentSet.forEach((doc, store) -> document.append(store.action, doc));
        }
    }

}
