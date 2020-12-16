package services.vortex.toastr.backend.redis;

import com.google.gson.JsonObject;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import redis.clients.jedis.*;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.resolver.Resolver;
import services.vortex.toastr.utils.PubSubEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RedisManager {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    public static final String CHANNEL_ALERT = "toastr-alert";
    public static final String CHANNEL_SENDTOALL = "toastr-sendtoall";
    private static final int CACHE_RESOLVER = 3600 * 6;
    private static final int CACHE_UUID = 3600 * 24;

    private final PubSubListener psListener;
    private final JedisPool pool;
    private final ScheduledTask updateTask, inconsistencyProxyTask;

    @Getter
    private int onlinePlayers;
    @Getter
    private final Set<String> knownProxies = new HashSet<>();
    @Getter
    private final String proxyName;

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

        updateTask = instance.getProxy().getScheduler().buildTask(instance, this::updatePlayerCounts).repeat(1, TimeUnit.SECONDS).schedule();
        inconsistencyProxyTask = instance.getProxy().getScheduler().buildTask(instance, this::fixPlayerProxyInconsistency).delay(5, TimeUnit.SECONDS).repeat(30, TimeUnit.SECONDS).schedule();
    }

    public void shutdown() {
        updateTask.cancel();
        inconsistencyProxyTask.cancel();

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
                    knownProxies.remove(proxy.toLowerCase());
                    instance.getLogger().warn("No heartbeat from " + proxy + " in 15 seconds, removing proxy.");
                    continue;
                }

                if(System.currentTimeMillis() - Long.parseLong(proxies.get(proxy)) > TimeUnit.SECONDS.toMillis(5)) {
                    instance.getLogger().warn("No heartbeat from " + proxy + " in 5 seconds, ignoring players.");
                    knownProxies.remove(proxy.toLowerCase());
                    continue;
                }

                knownProxies.add(proxy.toLowerCase());
                onlines += jedis.hlen("proxy:" + proxy + ":onlines");
            }

            jedis.hset("proxies", proxyName, Long.toString(System.currentTimeMillis()));
        }

        onlinePlayers = onlines;
    }

    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        return map.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void fixPlayerProxyInconsistency() {
        long start = System.currentTimeMillis();
        int count = 0;
        try(Jedis jedis = getConnection(); final Pipeline pipe = jedis.pipelined()) {
            final Set<String> onlineUUIDs = jedis.hkeys("proxy:" + proxyName + ":onlines");
            for(String uuid : onlineUUIDs) {
                if(!instance.getProxy().getPlayer(UUID.fromString(uuid)).isPresent()) {
                    instance.getLogger().warn("Removing " + uuid + " because it's stored in redis but not online");
                    pipe.hdel("proxy:" + proxyName + ":onlines", uuid);
                    count++;
                }
            }
            pipe.sync();
        }
        if(count == 0) return;

        instance.getLogger().warn("Removed " + count + " players due to inconsistency between in-game and redis in " + (System.currentTimeMillis() - start) + "ms");

        try(Jedis jedis = getConnection(); final Pipeline pipe = jedis.pipelined()) {
            final Map<String, String> stored = jedis.hgetAll("proxy:" + proxyName + ":onlines");
            final Set<String> storedPlayers = new HashSet<>(stored.values());
            if(storedPlayers.size() != stored.size()) {
                instance.getLogger().warn("there are duplicate players in the proxy... kicking them and removing duplicates");
                for(String player : storedPlayers) {
                    final Set<String> playerUUIDs = getKeysByValue(stored, player);
                    if(playerUUIDs.size() == 1) {
                        continue;
                    }

                    for(String uuid : playerUUIDs) {
                        pipe.hdel("proxy:" + proxyName + ":onlines", uuid);
                        instance.getLogger().warn("removed " + uuid + " from proxy");
                        instance.getProxy().getPlayer(UUID.fromString(uuid))
                                .ifPresent(proxyPlayer ->
                                        proxyPlayer.disconnect(Component.text("duplicate player consistency issue").color(NamedTextColor.RED))
                                );
                    }
                }
            }
            pipe.sync();
        }
    }

    /*
     * Implement another anti inconsistency
     *
     * For all servers in redis:
     *   * INTERSECT (https://redis.io/commands/sinter) each server pair
     *
     *   * OR look through each server and make a UNION (https://redis.io/commands/sunion)
     *   of everything except current server. Check if it exists.
     *   and remove players that are repeated
     * */

    /**
     * This method creates a new uuid and sets all the info
     *
     * @param uuid     The Player UUID
     * @param username The Player username
     * @param ip       The Player IP
     */
    public void createPlayer(UUID uuid, String username, String ip) {
        final Map<String, String> playerData = new HashMap<>();
        playerData.put("ip", ip);
        playerData.put("lastOnline", "0");
        playerData.put("proxy", proxyName);
        playerData.put("username", username);

        final HashMap<String, String> proxyData = new HashMap<>();
        proxyData.put(uuid.toString(), username);

        try(Jedis jedis = getConnection()) {
            final Transaction tx = jedis.multi();

            tx.hmset("proxy:" + proxyName + ":onlines", proxyData);
            tx.hmset("player:" + uuid, playerData);

            tx.exec();
        }
    }

    /**
     * This method sets the server where a Player is
     *
     * @param uuid     The Player's UUID
     * @param username The Player's username
     * @param server   The server
     */
    public void setPlayerServer(UUID uuid, String username, String server) {
        try(Jedis jedis = getConnection()) {
            String previous = jedis.hget("player:" + uuid, "server");
            if(previous != null) {
                jedis.hdel("server:" + proxyName + ":" + previous, uuid.toString());
            }

            jedis.hset("player:" + uuid, "server", server);
            jedis.hset("server:" + proxyName + ":" + server, uuid.toString(), username);
        }
    }

    /**
     * This method cleans all the info of a Player
     *
     * @param uuid The UUID of the Player
     */
    public void cleanPlayer(UUID uuid) {
        try(Jedis jedis = getConnection()) {
            String previous = jedis.hget("player:" + uuid, "server");
            if(previous != null) {
                jedis.hdel("server:" + proxyName + ":" + previous, uuid.toString());
            }

            jedis.hdel("proxy:" + proxyName + ":onlines", uuid.toString());
            jedis.hset("player:" + uuid, "lastOnline", Long.toString(System.currentTimeMillis()));
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
            jedis.expire("resolver:" + username.toLowerCase(), CACHE_RESOLVER);

            jedis.setex("playeruuid:" + username.toLowerCase(), CACHE_UUID, result.getPlayerUUID().toString());
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
     * This method gets all the online players in <strong>all the online proxies</strong>
     *
     * @return A Set of Strings with all the Players from all the proxies, null if proxy not found
     */
    public Set<String> getUsernamesOnline() {
        String[] keys = new String[knownProxies.size()];
        int i = 0;
        for(String proxy : knownProxies) {
            keys[i] = "proxy:" + proxy + ":onlines";
        }

        List<Response<List<String>>> results = new ArrayList<>(keys.length);
        try(Jedis jedis = getConnection()) {
            final Pipeline pipe = jedis.pipelined();
            for(String key : keys) {
                results.add(pipe.hvals(key));
            }
            pipe.sync();
        }

        Set<String> onlines = new HashSet<>();
        for(Response<List<String>> result : results) {
            onlines.addAll(result.get());
        }

        return onlines;
    }

    // TODO: getProxyData & getProxyUsernames, getProxyUUIDs

    /**
     * This method gets the player count in a proxy
     *
     * @param proxy The proxy name
     * @return The player count, null if proxy not found
     */
    public Long getProxyCount(String proxy) {
        try(Jedis jedis = getConnection()) {
            return jedis.hlen("proxy:" + proxy + ":onlines");
        }
    }

    /**
     * This method gets the online players in a specified server
     *
     * @param server The server name
     * @return A list of the players in the server (UUID)
     */
    public HashMap<UUID, String> getServerData(String server) {
        String[] keys = new String[knownProxies.size()];
        int i = 0;
        for(String proxy : knownProxies) {
            keys[i] = "server:" + proxy + ":" + server;
        }

        List<Response<Map<String, String>>> results = new ArrayList<>(keys.length);
        try(Jedis jedis = getConnection()) {
            final Pipeline pipe = jedis.pipelined();
            for(String key : keys) {
                results.add(pipe.hgetAll(key));
            }
            pipe.sync();
        }

        HashMap<UUID, String> onlines = new HashMap<>();
        for(Response<Map<String, String>> result : results) {
            final Map<String, String> data = result.get();
            for(String uuid : data.keySet()) {
                onlines.put(UUID.fromString(uuid), data.get(uuid));
            }
        }

        return onlines;
    }


    /**
     * This method gets the online players in a specified server
     *
     * @param server The server name
     * @return A list of the players in the server (UUID)
     */
    public Set<UUID> getServerUUIDs(String server) {
        String[] keys = new String[knownProxies.size()];
        int i = 0;
        for(String proxy : knownProxies) {
            keys[i] = "server:" + proxy + ":" + server;
        }

        List<Response<Set<String>>> results = new ArrayList<>(keys.length);
        try(Jedis jedis = getConnection()) {
            final Pipeline pipe = jedis.pipelined();
            for(String key : keys) {
                results.add(pipe.hkeys(key));
            }
            pipe.sync();
        }

        Set<UUID> onlines = new HashSet<>();
        for(Response<Set<String>> result : results) {
            for(String s : result.get()) {
                onlines.add(UUID.fromString(s));
            }
        }

        return onlines;
    }

    /**
     * This method gets the online players in a specified server
     *
     * @param server The server name
     * @return A list of the players in the server (UUID)
     */
    public Set<String> getServerUsernames(String server) {
        String[] keys = new String[knownProxies.size()];
        int i = 0;
        for(String proxy : knownProxies) {
            keys[i] = "server:" + proxy + ":" + server;
        }

        List<Response<List<String>>> results = new ArrayList<>(keys.length);
        try(Jedis jedis = getConnection()) {
            final Pipeline pipe = jedis.pipelined();
            for(String key : keys) {
                results.add(pipe.hvals(key));
            }
            pipe.sync();
        }

        Set<String> onlines = new HashSet<>();
        for(Response<List<String>> result : results) {
            onlines.addAll(result.get());
        }

        return onlines;
    }

    /**
     * This method gets the player count in a server
     *
     * @param server The proxy name
     * @return The player count, null if server not found
     */
    public int getServerCount(String server) {
        String[] keys = new String[knownProxies.size()];
        int i = 0;
        for(String proxy : knownProxies) {
            keys[i] = "server:" + proxy + ":" + server;
        }

        int online = 0;
        try(Jedis jedis = getConnection()) {
            for(String key : keys) {
                online += jedis.hlen(key);
            }
        }

        return online;
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
