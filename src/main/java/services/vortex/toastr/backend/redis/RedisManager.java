package services.vortex.toastr.backend.redis;

import com.google.gson.JsonObject;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.Getter;
import redis.clients.jedis.*;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.resolver.Resolver;
import services.vortex.toastr.utils.PubSubEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedisManager {
    public static final String CHANNEL_ALERT = "toastr-alert";
    public static final String CHANNEL_SENDTOALL = "toastr-sendtoall";
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final int RESOLVER_CACHE_TIME = 3600 * 6;
    private final PubSubListener psListener;
    @Getter
    private final String proxyName;
    private final JedisPool pool;
    private final ScheduledTask updateTask, inconsistencyTask;
    @Getter
    private int onlinePlayers;

    public RedisManager() {
        JsonObject redisConfig = instance.getConfig().getObject().getAsJsonObject("redis");

        if(redisConfig.get("password").getAsString().isEmpty()) {
            pool = new JedisPool(new JedisPoolConfig(), redisConfig.get("host").getAsString(), redisConfig.get("port").getAsInt());
        } else {
            pool = new JedisPool(new JedisPoolConfig(), redisConfig.get("host").getAsString(), redisConfig.get("port").getAsInt(), 2000, redisConfig.get("password").getAsString());
        }

        proxyName = instance.getConfig().getObject().get("proxy-name").getAsString();

        psListener = new PubSubListener();
        Thread subscribeThread = new Thread(() -> {
            try(Jedis jedis = getConnection()) {
                jedis.subscribe(psListener, RedisManager.CHANNEL_ALERT, RedisManager.CHANNEL_SENDTOALL);
            }
        }, "Toastr PubSub subscriber");
        subscribeThread.setDaemon(true);
        subscribeThread.start();

        updateTask = instance.getProxy().getScheduler().buildTask(instance, this::updatePlayerCounts).delay(1, TimeUnit.SECONDS).repeat(1, TimeUnit.SECONDS).schedule();
        inconsistencyTask = instance.getProxy().getScheduler().buildTask(instance, this::fixInconsistency).delay(5, TimeUnit.SECONDS).repeat(30, TimeUnit.SECONDS).schedule();
    }

    public void shutdown() {
        updateTask.cancel();
        inconsistencyTask.cancel();

        pool.close();
    }

    private void updatePlayerCounts() {
        int onlines = 0;

        try(Jedis jedis = getConnection()) {
            Map<String, String> proxies = jedis.hgetAll("proxies");
            for(String proxy : proxies.keySet()) {
                if(System.currentTimeMillis() - Long.parseLong(proxies.get(proxy)) > TimeUnit.SECONDS.toMillis(15)) {
                    jedis.hdel("proxies", proxy);
                    jedis.del(" proxy:" + proxy + ":onlines");
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
    }

    private void fixInconsistency() {
        long start = System.currentTimeMillis();
        try(Jedis jedis = getConnection(); final Pipeline pipe = jedis.pipelined()) {
            final Set<String> stored = jedis.smembers("proxy:" + proxyName + ":onlines");
            int count = 0;

            for(String storedPlayer : stored) {
                UUID storedUUID;
                try {
                    storedUUID = UUID.fromString(storedPlayer);
                } catch(IllegalArgumentException ignore) {
                    continue;
                }

                if(!instance.getProxy().getPlayer(storedUUID).isPresent()) {
                    instance.getLogger().warn("Removing " + storedPlayer + " because it's stored in redis but not online");
                    pipe.srem("proxy:" + proxyName + ":onlines", storedPlayer);
                    count++;
                }
            }
            pipe.sync();

            if(count != 0)
                instance.getLogger().warn("Removed " + count + " players due to inconsistency between in-game and redis in " + (System.currentTimeMillis() - start) + "ms");
        }
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
            final Transaction tx = jedis.multi();

            tx.sadd("proxy:" + proxyName + ":onlines", player.toString());
            tx.hmset("player:" + player, data);

            tx.set("playeruuid:" + name.toLowerCase(), player.toString());

            tx.exec();
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
    // TODO: optimize this wtf?
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

    public void publishMessage(String channel, String message) {
        try(Jedis jedis = getConnection()) {
            jedis.publish(channel, message);
        }
    }

    private Jedis getConnection() {
        return pool.getResource();
    }

    static class PubSubListener extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            instance.getProxy().getEventManager().fireAndForget(new PubSubEvent(channel, message));
        }

    }

}
