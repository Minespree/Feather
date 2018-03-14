package net.minespree.feather.player.save;

import java.util.concurrent.CompletableFuture;

public interface PlayerSaver {
    CompletableFuture<Boolean> save(Saveable saveable);
}
