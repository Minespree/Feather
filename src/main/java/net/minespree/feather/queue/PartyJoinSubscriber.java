package net.minespree.feather.queue;

import lombok.AllArgsConstructor;
import redis.clients.jedis.JedisPubSub;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public class PartyJoinSubscriber extends JedisPubSub {
    public static final String PARTY_CHANNEL = "msPartyJoin";
    private static final String SEPARATOR = "###";

    private final PartyJoinManager manager;
    private final String serverName;

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals(PARTY_CHANNEL)) {
            return;
        }

        String[] args = message.split(SEPARATOR);

        // Only 1 party member
        if (args.length <= 2) {
            return;
        }

        // Other server, ignore
        if (!serverName.equals(args[0])) {
            return;
        }

        List<UUID> uuidList = Arrays.stream(args, 1, args.length)
                .map(uuidString -> {
                    try {
                        return UUID.fromString(uuidString);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        PartyJoin join = new PartyJoin(uuidList);

        manager.addPartyJoin(join);
    }
}
