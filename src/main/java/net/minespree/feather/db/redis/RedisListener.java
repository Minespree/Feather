package net.minespree.feather.db.redis;

import org.json.simple.JSONObject;

public interface RedisListener {

    void receive(String channel, JSONObject object);

}
