package net.minespree.feather.db.redis;

import lombok.Getter;
import net.minespree.feather.FeatherPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RedisManager {
    private static final RedisManager MANAGER = new RedisManager();

    @Getter
    private JedisPool pool;

    private Map<String, RedisListener> listenerMap = new ConcurrentHashMap<>();
    private String host;
    private int port;

    private RedisManager() {
    }

    public static RedisManager getInstance() {
        return MANAGER;
    }

    public void init(String host, int port) {
        this.host = host;
        this.port = port;

        pool = new JedisPool(new JedisPoolConfig(), host, port);

        new Thread(this::runSubscriber, "Redis Subscriber Thread").start();
    }

    private void runSubscriber() {
        int secs = 1;
        while (true) {
            Jedis subscriber = null;
            try {
                subscriber = new Jedis(host, port);
                secs = 1;
                subscriber.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            JSONObject object = (JSONObject) new JSONParser().parse(message);
                            String subchannel = (String) object.remove("channel");

                            RedisListener rl = listenerMap.get(subchannel);
                            if (rl != null) {
                                rl.receive(subchannel, object);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, "feather");
            } catch (JedisConnectionException e) {
                if (subscriber != null) {
                    subscriber.close();
                }
                FeatherPlugin.get().getLogger().severe("Lost Redis connection, going to retry in " + secs + " seconds...");
                try {
                    Thread.sleep(secs * 1000);
                } catch (InterruptedException e1) {
                    return;
                }
                secs += secs;
            }
        }
    }

    public void registerListener(RedisListener listener, String... channels) {
        Arrays.stream(channels).forEach(channel -> {
            listenerMap.putIfAbsent(channel, listener);
            Logger.getGlobal().log(Level.INFO, "Registered channel listener for \"" + channel + "\"");
        });
    }

    public void registerListener(String channel, RedisListener listener) {
        listenerMap.putIfAbsent(channel, listener);
        Logger.getGlobal().log(Level.INFO, "Registered channel listener for \"" + channel + "\"");
    }

    public void post(JSONObject object) {
        try (Jedis publisher = pool.getResource()) {
            publisher.publish("feather", object.toJSONString());
        }
    }
}
