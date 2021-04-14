package dev.sergivos.toastr.backend;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.profile.PlayerData;
import dev.sergivos.toastr.resolver.Resolver;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CacheManager {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private final Cache<String, Resolver.Result> resolver = CacheBuilder.newBuilder()
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .initialCapacity(750)
            .maximumSize(1000)
            .build();
    private final AtomicReference<Set<String>> allOnline = new AtomicReference<>();
    private long lastAllOnline = 0;

    /**
     * This method gets all online player's usernames, if not cached it gets data from redis
     *
     * @return Set of usernames
     */
    // TODO: clean this up
    public Set<String> getUsernamesOnline() {
        if((System.currentTimeMillis() - lastAllOnline) > TimeUnit.SECONDS.toMillis(5)) {
            allOnline.set(instance.getRedisManager().getUsernamesOnline());
            lastAllOnline = System.currentTimeMillis();
        }

        return allOnline.get();
    }

    /**
     * This method gets the Resolver Result of a username, if not cached it gets the data from redis
     *
     * @param username The username of the Player
     * @return the Resolver.Result, null if not found
     */
    public Resolver.Result getPlayerResult(String username) {
        Resolver.Result result = resolver.getIfPresent(username.toLowerCase());
        if(result == null) {
            result = instance.getRedisManager().getPlayerResult(username.toLowerCase());
            if(result == null)
                return null;

            resolver.put(username.toLowerCase(), result);
        }

        return result;
    }

    /**
     * This method gets a PlayerData of a Player from the Cache, if not cached it gets the data from redis
     *
     * @param username The username of the Player
     * @return The PlayerData, null if not found
     */
    public PlayerData getPlayerData(String username) {
        UUID uuid = getUUID(username);
        if(uuid == null)
            return null;

        return instance.getRedisManager().getPlayer(uuid);
    }

    /**
     * This method gets a PlayerData of a Player from the Cache, if not cached it gets the data from redis
     *
     * @param playerUUID The name of the Player
     * @return The PlayerData, null if not found
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        return instance.getRedisManager().getPlayer(playerUUID);
    }

    /**
     * This method gets the UUID of a Player from the Cache, if not cached it gets the data from redis
     *
     * @param username The username of the Player
     * @return The UUID, null if not found
     */
    private UUID getUUID(String username) {
        final Player player = instance.getProxy().getPlayer(username).orElse(null);
        if(player != null)
            return player.getUniqueId();

        Resolver.Result result = getPlayerResult(username);
        if(result == null) {
            result = instance.getRedisManager().getPlayerResult(username);
            if(result == null)
                return null;
        }

        return result.getUniqueId();
    }

    public void clearCache(String username) {
        resolver.invalidate(username);
    }

}
