package net.minespree.feather.db.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoManager {
    private static final MongoManager instance = new MongoManager();
    private MongoClient client;
    private MongoDatabase db;

    public static MongoManager getInstance() {
        return instance;
    }

    public void createConnection(String host, int port, String databse, String user, String pass) {
        client = new MongoClient(Collections.singletonList(new ServerAddress(host, port)),
                Collections.singletonList(MongoCredential.createCredential(user, databse, pass.toCharArray())));
        //  client = new MongoClient(new ServerAddress(host, port), Collections.singletonList(MongoCredential.createCredential(user, databse, pass.toCharArray())));
        db = client.getDatabase(databse);

        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);
    }

    public MongoClient getClient() {
        return client;
    }

    public MongoDatabase getDb() {
        return db;
    }

    public MongoCollection<Document> getCollection(String name) {
        return db.getCollection(name);
    }
}
