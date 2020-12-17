package services.vortex.toastr.backend.redis;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.resolver.Resolver;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private final Cache<String, UUID> uuids = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(250)
            .build();
    private final Cache<UUID, PlayerData> players = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(50)
            .build();
    private final Cache<String, Resolver.Result> resolver = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(250)
            .build();
    private Set<String> allOnline = new HashSet<>();
    private long lastAllOnline = 0;

    /**
     * This method gets all online player's usernames, if not cached it gets data from redis
     *
     * @return Set of usernames
     */
    // TODO: clean this up
    public Set<String> getUsernamesOnline() {
        if((System.currentTimeMillis() - lastAllOnline) > TimeUnit.SECONDS.toMillis(5)) {
            allOnline = instance.getRedisManager().getUsernamesOnline();
            lastAllOnline = System.currentTimeMillis();
        }

        return allOnline;
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
     * @param uuid The UUID of the Player
     * @return The PlayerData, null if not found
     */
    public PlayerData getPlayerData(UUID uuid) {
        PlayerData data = players.getIfPresent(uuid);
        if(data == null) {
            data = instance.getRedisManager().getPlayer(uuid);
            if(data == null)
                return null;

            players.put(uuid, data);
        }

        return data;
    }

    /**
     * This method gets a PlayerData of a Player from the Cache, if not cached it gets the data from redis
     *
     * @param name The name of the Player
     * @return The PlayerData, null if not found
     */
    public PlayerData getPlayerData(String name) {
        UUID uuid = getUUID(name);
        if(uuid == null)
            return null;

        return getPlayerData(uuid);
    }

    /**
     * This method gets the UUID of a Player from the Cache, if not cached it gets the data from redis
     *
     * @param name The name of the Player
     * @return The UUID, null if not found
     */
    private UUID getUUID(String name) {
        UUID uuid = uuids.getIfPresent(name);
        if(uuid == null) {
            uuid = instance.getRedisManager().getPlayerUUID(name);
            if(uuid == null)
                return null;

            uuids.put(name.toLowerCase(), uuid);
        }

        return uuid;
    }

    public void clearCache(String username) {
        uuids.invalidate(username);
        resolver.invalidate(username);
    }

}
