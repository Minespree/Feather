package net.minespree.feather.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import net.minespree.feather.db.mongo.MongoManager;
import net.minespree.feather.util.BSONUtil;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

public final class Friends {
    private static final String COLLECTION_NAME = "relationships";

    private Friends() {
        throw new UnsupportedOperationException("Friends cannot be instantiated!");
    }

    public static void get(UUID uniqueId, Status status, FutureCallback<Set<UUID>> callback) {
        get(uniqueId, status, callback, MoreExecutors.sameThreadExecutor());
    }

    public static void get(UUID uniqueId, Status status, FutureCallback<Set<UUID>> callback, Executor executor) {
        executor.execute(() -> {
            MongoCollection<Document> collection = MongoManager.getInstance().getCollection(COLLECTION_NAME);
            ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();
            BiFunction<MongoCollection<Document>, UUID, MongoCursor<Document>> function = status == Status.FRIENDS ? Friends::aggregateFriends : Friends::getRequests;
            try (MongoCursor<Document> cursor = function.apply(collection, uniqueId)) {
                while (cursor.hasNext()) {
                    Document current = cursor.next();
                    List<Object> list = (List<Object>) current.get(status.fieldName);
                    if (list != null && !list.isEmpty()) {
                        list.stream()
                                .filter(Objects::nonNull)
                                .map(String::valueOf)
                                .map(UUID::fromString)
                                .forEach(builder::add);
                    }
                }
                callback.onSuccess(builder.build());
            } catch (Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    public static void createRequest(UUID from, UUID to, FutureCallback<Response> callback, Executor executor) {
        executor.execute(() -> {
            MongoCollection<Document> collection = MongoManager.getInstance().getCollection(COLLECTION_NAME);
            try {
                Document query = new Document("_id", to.toString());
                query.put(Status.FRIENDS.fieldName, new Document("$elemMatch", new Document("$eq", from.toString())));
                Document found = collection.find(query).first();
                if (found != null) {
                    callback.onSuccess(Response.FAILED_ALREADY_FRIENDS);
                    return;
                }

                Document changes = new Document("$addToSet", new Document(Status.PENDING.fieldName, from.toString()));

                UpdateResult result = collection.updateOne(Filters.eq("_id", to.toString()), changes, new UpdateOptions().upsert(true));

                if (result.getModifiedCount() > 0) {
                    callback.onSuccess(Response.SUCCESS);
                } else {
                    callback.onSuccess(Response.FAILED_ALREADY_EXIST);
                }
            } catch (Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    public static void deleteRequest(UUID from, UUID to, FutureCallback<Response> callback, Executor executor) {
        executor.execute(() -> {
            MongoCollection<Document> collection = MongoManager.getInstance().getCollection(COLLECTION_NAME);
            try {
                Document query = new Document("_id", from.toString());
                Document changes = new Document("$pull", new Document(Status.PENDING.fieldName, to.toString()));

                com.mongodb.bulk.BulkWriteResult result = collection.bulkWrite(
                        Collections.singletonList(
                                new UpdateOneModel<>(query, changes)
                        )
                );

                if (result.isModifiedCountAvailable() && result.getModifiedCount() > 0) {
                    callback.onSuccess(Response.SUCCESS);
                } else {
                    callback.onSuccess(Response.FAILED_NO_REQUEST);
                }
            } catch (Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    public static void createFriendship(UUID from, UUID to, FutureCallback<Response> callback, Executor executor) {
        executor.execute(() -> {
            MongoCollection<Document> collection = MongoManager.getInstance().getCollection(COLLECTION_NAME);
            try {
                Document query = new Document("_id", from.toString());
                query.put(Status.PENDING.fieldName, new Document("$elemMatch", new Document("$eq", to.toString())));
                MongoCursor<Document> findResult = collection.find(query).iterator();
                if (!findResult.hasNext()) {
                    callback.onSuccess(Response.FAILED_NO_REQUEST);
                    return;
                }

                query = new Document("_id", from.toString());
                query.put(Status.FRIENDS.fieldName, new Document("$elemMatch", new Document("$eq", to.toString())));
                if (collection.find(query.append("_id", 1)).iterator().hasNext()) {
                    callback.onSuccess(Response.FAILED_ALREADY_FRIENDS);
                    return;
                }

                UpdateOneModel<Document> updateOne = new UpdateOneModel<>(BSONUtil.getIdentifier(to), new Document("$addToSet", new Document(Status.FRIENDS.fieldName, from.toString())));
                updateOne.getOptions().upsert(true);

                UpdateOneModel<Document> updateTwo = new UpdateOneModel<>(BSONUtil.getIdentifier(from), new Document("$addToSet", new Document(Status.FRIENDS.fieldName, to.toString())).append("$pull", new Document(Status.PENDING.fieldName, to.toString())));
                updateTwo.getOptions().upsert(true);

                com.mongodb.bulk.BulkWriteResult result = collection.bulkWrite(
                        Arrays.asList(
                                updateOne,
                                updateTwo
                        )
                );

                if (result.getUpserts().size() > 0 || result.getModifiedCount() > 0) {
                    callback.onSuccess(Response.SUCCESS);

                    Player pOne = Bukkit.getPlayer(from);
                    Player pTwo = Bukkit.getPlayer(to);

                    if (pOne != null) {
                        NetworkPlayer.of(pOne).addFriend(to);
                    }

                    if (pTwo != null) {
                        NetworkPlayer.of(pTwo).addFriend(from);
                    }
                } else {
                    callback.onSuccess(Response.FAILED_ALREADY_EXIST);
                }
            } catch (Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    public static void betray(UUID from, UUID to, FutureCallback<Response> callback, Executor executor) {
        executor.execute(() -> {
            MongoCollection<Document> collection = MongoManager.getInstance().getCollection(COLLECTION_NAME);
            try {
                com.mongodb.bulk.BulkWriteResult result = collection.bulkWrite(Arrays.asList(
                        new UpdateOneModel<>(BSONUtil.getIdentifier(from), new Document("$pull", new Document(Status.FRIENDS.fieldName, to.toString()))),
                        new UpdateOneModel<>(BSONUtil.getIdentifier(to), new Document("$pull", new Document(Status.FRIENDS.fieldName, from.toString())))
                ));

                if (result.isModifiedCountAvailable() && result.getModifiedCount() > 0) {
                    callback.onSuccess(Response.SUCCESS);

                    Player pOne = Bukkit.getPlayer(from);
                    Player pTwo = Bukkit.getPlayer(to);

                    if (pOne != null) {
                        NetworkPlayer.of(pOne).removeFriend(to);
                    }

                    if (pTwo != null) {
                        NetworkPlayer.of(pTwo).removeFriend(from);
                    }
                } else {
                    callback.onSuccess(Response.FAILED_NOT_FRIENDS);
                }
            } catch (Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    private static MongoCursor<Document> aggregateFriends(MongoCollection<Document> collection, UUID uniqueId) {
        List<Document> pipeline = ImmutableList.of(match(uniqueId, Status.FRIENDS), group(uniqueId, Status.FRIENDS));
        return collection.aggregate(pipeline).allowDiskUse(true).iterator();
    }

    private static MongoCursor<Document> getRequests(MongoCollection<Document> collection, UUID uniqueId) {
        return collection.find(new Document("_id", uniqueId).append(Status.PENDING.fieldName, 1)).iterator();
    }

    private static Document match(UUID uniqueId, Status status) {
        return new Document("$match", new Document(status.fieldName, new Document("$elemMatch", new Document("$eq", uniqueId.toString()))));
    }

    private static Document group(UUID uniqueId, Status status) {
        return new Document("$group", new Document("_id", uniqueId.toString()).append(status.fieldName, new Document("$push", "$_id")));
    }

    public enum Status {
        PENDING("friend_requests"),
        FRIENDS("friends");

        private final String fieldName;

        Status(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    public enum Response {
        SUCCESS,
        FAILED_ALREADY_EXIST,
        FAILED_ALREADY_FRIENDS,
        FAILED_NO_REQUEST,
        FAILED_NOT_FRIENDS
    }
}
