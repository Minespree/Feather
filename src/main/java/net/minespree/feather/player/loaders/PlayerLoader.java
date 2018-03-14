package net.minespree.feather.player.loaders;

import net.minespree.feather.player.NetworkPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface PlayerLoader {
    <T extends NetworkPlayer> CompletableFuture<T> loadPlayer(UUID uuid, Supplier<T> supplier, boolean bootstrap);
}
