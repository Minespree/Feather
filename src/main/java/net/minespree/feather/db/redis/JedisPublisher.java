package net.minespree.feather.db.redis;

import org.json.simple.JSONObject;

public class JedisPublisher {
    private String channel;
    private JSONObject message;

    private JedisPublisher(String channel) {
        this.channel = channel;
        this.message = new JSONObject();
    }

    public static JedisPublisher create(String channel) {
        return new JedisPublisher(channel);
    }

    public JedisPublisher set(String field, Object value) {
        message.put(field, String.valueOf(value));
        return this;
    }

    public void publish() {
        message.put("channel", channel);
        RedisManager.getInstance().post(message);
    }
}
