package services.vortex.toastr.backend.redis;

import com.google.gson.JsonObject;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.listeners.PubSubListener;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.resolver.Resolver;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedisManager {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final int RESOLVER_CACHE_TIME = 3600 * 6;
    private PubSubListener psListener;

    @Getter
    private String proxyName;
    @Getter
    private int onlinePlayers;

    private JedisPool pool;

    /**
     * This method enables the RedisManager
     */
    public void enable() {
        JsonObject redisConfig = instance.getConfig().getObject().getAsJsonObject("redis");

        if(redisConfig.get("password").getAsString().isEmpty()) {
            pool = new JedisPool(new JedisPoolConfig(), redisConfig.get("host").getAsString(), redisConfig.get("port").getAsInt());
        } else {
            pool = new JedisPool(new JedisPoolConfig(), redisConfig.get("host").getAsString(), redisConfig.get("port").getAsInt(), 2000, redisConfig.get("password").getAsString());
        }

        psListener = new PubSubListener();

        proxyName = instance.getConfig().getObject().get("proxy-name").getAsString();

        instance.getProxy().getScheduler().buildTask(instance, () -> {
            int onlines = 0;

            try(Jedis jedis = getConnection()) {
                Map<String, String> proxies = jedis.hgetAll("proxies");
                for(String proxy : proxies.keySet()) {
                    if(System.currentTimeMillis() - Long.parseLong(proxies.get(proxy)) > TimeUnit.SECONDS.toMillis(15)) {
                        jedis.hdel("proxies", proxy);
                        instance.getLogger().warn("No heartbeat from " + proxy + " in 15 seconds, removing proxy.");
                        continue;
                    }

                    if(System.currentTimeMillis() - Long.parseLong(proxies.get(proxy)) > TimeUnit.SECONDS.toMillis(5)) {
                        instance.getLogger().warn("No heartbeat from " + proxy + " in 5 seconds, ignoring players.");
                        continue;
                    }

                    onlines += jedis.scard("proxy:" + proxy + ":onlines");
                }

                jedis.hset("proxies", proxyName, Long.toString(System.currentTimeMillis()));
            }

            onlinePlayers = onlines;
        }).repeat(1, TimeUnit.SECONDS).schedule();
    }

    public void shutdown() {
        pool.close();
    }

    /**
     * This method creates a new player and sets all the info
     *
     * @param player The Player UUID
     * @param name   The Player name
     * @param ip     The Player IP
     */
    public void createPlayer(UUID player, String name, String ip) {
        Map<String, String> data = new HashMap<>();
        data.put("ip", ip);
        data.put("lastOnline", "0");
        data.put("proxy", proxyName);

        try(Jedis jedis = getConnection()) {
            jedis.sadd("proxy:" + proxyName + ":onlines", player.toString());
            jedis.hmset("player:" + player, data);

            jedis.set("playeruuid:" + name.toLowerCase(), player.toString());
        }
    }

    /**
     * This method sets the server where a Player is
     *
     * @param player The Player's UUID
     * @param server The server
     */
    public void setPlayerServer(UUID player, String server) {
        try(Jedis jedis = getConnection()) {
            jedis.hset("player:" + player, "server", server);
        }
    }

    /**
     * This method cleans all the info of a Player
     *
     * @param player The UUID of the Player
     */
    public void cleanPlayer(UUID player) {
        try(Jedis jedis = getConnection()) {
            jedis.srem("proxy:" + proxyName + ":onlines", player.toString());
            jedis.hdel("player:" + player, "server", "ip", "proxy");

            jedis.hset("player:" + player, "lastOnline", Long.toString(System.currentTimeMillis()));
        }
    }

    /**
     * This method gets the UUID of a Player
     *
     * @param player The Player name
     * @return The UUID, null if not found
     */
    public UUID getPlayerUUID(String player) {
        try(Jedis jedis = getConnection()) {
            String s = jedis.get("playeruuid:" + player.toLowerCase());
            if(s == null)
                return null;

            return UUID.fromString(s);
        }
    }

    /**
     * This method gets the PlayerData of a Player
     *
     * @param uuid The UUID of the Player
     * @return The PlayerData, null if not found
     */
    public PlayerData getPlayer(UUID uuid) {
        try(Jedis jedis = getConnection()) {
            Map<String, String> data = jedis.hgetAll("player:" + uuid);
            if(data == null || data.isEmpty())
                return null;

            return new PlayerData(
                    uuid,
                    Long.parseLong(data.get("lastOnline")),
                    data.getOrDefault("ip", null),
                    data.getOrDefault("proxy", null),
                    data.getOrDefault("server", null)
            );
        }
    }

    public void setPlayerResult(String username, Resolver.Result result) {
        try(Jedis jedis = getConnection()) {
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("uuid", result.getPlayerUUID().toString());
            data.put("premium", String.valueOf(result.isPremium()));
            data.put("spoofed", String.valueOf(result.isSpoofed()));
            data.put("source", result.getSource());

            jedis.hset("resolver:" + username.toLowerCase(), data);
            jedis.expire("resolver:" + username.toLowerCase(), RESOLVER_CACHE_TIME);
        }
    }

    public Resolver.Result getPlayerResult(String username) {
        try(Jedis jedis = getConnection()) {
            final Map<String, String> result = jedis.hgetAll("resolver:" + username.toLowerCase());

            if(result == null || result.isEmpty()) {
                return null;
            }

            return new Resolver.Result(result.get("username"), UUID.fromString(result.get("uuid")),
                    Boolean.parseBoolean(result.get("premium")), Boolean.parseBoolean(result.get("spoofed")), result.get("source"));
        }
    }

    /**
     * This method gets all the online players in a proxy
     *
     * @param proxy The proxy name
     * @return A Set of Strings with all the Players, null if proxy not found
     */
    public Set<String> getOnlinePlayers(String proxy) {
        try(Jedis jedis = getConnection()) {
            return jedis.smembers("proxy:" + proxy + ":onlines");
        }
    }

    /**
     * This method gets the online players in a specified server
     *
     * @param server The server name
     * @return A list of the players in the server (UUID)
     */
    public Set<UUID> getOnlinePlayersInServer(String server) {
        Set<UUID> onlines = new HashSet<>();

        try(Jedis jedis = getConnection()) {
            Map<String, String> proxies = jedis.hgetAll("proxies");
            for(String proxy : proxies.keySet()) {
                Set<String> players = instance.getCacheManager().getOnlinePlayers(proxy);
                for(String player : players) {
                    UUID uuid = UUID.fromString(player);
                    String playerServer = jedis.hget("player:" + uuid, "server");
                    if(playerServer.equalsIgnoreCase(server))
                        onlines.add(uuid);
                }
            }
        }

        return onlines;
    }

    /**
     * This method register a channel to be used in the PubSubEvent
     *
     * @param channels The channels to register
     */
    public void registerChannel(String... channels) {
        psListener.subscribe(channels);
    }

    /**
     * This method unregister a channel for the PubSubEvent
     *
     * @param channels The channels to unregister
     */
    public void unregisterChannel(String... channels) {
        psListener.unsubscribe(channels);
    }

    private Jedis getConnection() {
        return pool.getResource();
    }

}
