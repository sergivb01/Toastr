package services.vortex.toastr.backend.redis;

import com.google.gson.JsonObject;
import com.minexd.pidgin.Pidgin;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.Getter;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisNoScriptException;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.*;
import services.vortex.toastr.listeners.NetworkListener;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.resolver.Resolver;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RedisManager {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    private static final int CACHE_RESOLVER = 3600 * 2;
    private static final int CACHE_UUID = 3600 * 24 * 5;

    private final JedisPool pool;
    private final ScheduledTask updateTask, inconsistencyProxyTask, clockDifferenceTask;

    @Getter
    private final AtomicInteger onlinePlayers = new AtomicInteger(0);
    @Getter
    private final Set<String> knownProxies = new HashSet<>();
    @Getter
    private final String proxyName;
    @Getter
    private final Pidgin pidgin;

    public RedisManager() {
        JsonObject redisConfig = instance.getConfig().getObject().getAsJsonObject("redis");

        if(redisConfig.get("password").getAsString().isEmpty()) {
            pool = new JedisPool(new JedisPoolConfig(), redisConfig.get("host").getAsString(), redisConfig.get("port").getAsInt());
        } else {
            pool = new JedisPool(new JedisPoolConfig(), redisConfig.get("host").getAsString(), redisConfig.get("port").getAsInt(), 2000, redisConfig.get("password").getAsString());
        }

        proxyName = instance.getConfig().getObject().get("proxy-name").getAsString();

        pidgin = new Pidgin("toastr", redisConfig.get("host").getAsString(), redisConfig.get("port").getAsInt(), redisConfig.get("password").getAsString());
        pidgin.registerListener(new NetworkListener());
        Arrays.asList(
                AlertPacket.class,
                ClearCachePacket.class,
                CommandPacket.class,
                GlobalMessagePacket.class,
                KickPacket.class,
                NetworkStatusPacket.class,
                StaffJoinPacket.class,
                StaffQuitPacket.class,
                StaffSwitchPacket.class
        ).forEach(pidgin::registerPacket);

        instance.getProxy().getScheduler().buildTask(instance, ()-> pidgin.sendPacket(new NetworkStatusPacket(proxyName, true))).delay(1, TimeUnit.SECONDS).schedule();
        clockDifferenceTask = instance.getProxy().getScheduler().buildTask(instance, this::checkClockDifference).delay(5, TimeUnit.SECONDS).repeat(30, TimeUnit.SECONDS).schedule();
        inconsistencyProxyTask = instance.getProxy().getScheduler().buildTask(instance, this::fixPlayerProxyInconsistency).delay(10, TimeUnit.SECONDS).repeat(30, TimeUnit.SECONDS).schedule();
        updateTask = instance.getProxy().getScheduler().buildTask(instance, this::updatePlayerCounts).repeat(1, TimeUnit.SECONDS).schedule();
    }

    public void shutdown() {
        removeProxyInstance(proxyName);

        clockDifferenceTask.cancel();
        inconsistencyProxyTask.cancel();
        updateTask.cancel();

        pool.close();
    }

    public String createScript(String data) {
        try(final Jedis jedis = getConnection()) {
            return jedis.scriptLoad(data);
        }
    }

    public Object executeScript(LuaScripts script, List<String> keys, List<String> args) {
        Object data;
        try(final Jedis jedis = getConnection()) {
            data = jedis.evalsha(script.getHash(), keys, args);
        } catch(JedisNoScriptException ex) {
            try(final Jedis jedis = getConnection()) {
                data = jedis.eval(script.getScript(), keys, args);
            }
        }
        return data;
    }

    // TODO: migrate to redis lua script
    private void removeProxyInstance(String proxy) {
        final long start = System.currentTimeMillis();
        try(Jedis jedis = getConnection()) {
            final Map<String, String> onlines = jedis.hgetAll("proxy:" + proxy + ":onlines");
            knownProxies.remove(proxy.toLowerCase());

            try(final Pipeline pipe = jedis.pipelined()) {
                pipe.hdel("proxies", proxy);
                pipe.del("proxy:" + proxy + ":onlines");

                for(RegisteredServer server : instance.getProxy().getAllServers()) {
                    pipe.srem("server:" + server.getServerInfo().getName(), onlines.values().toArray(new String[0]));
                }
                for(String rawUUID : onlines.keySet()) {
                    pipe.hset("player:" + rawUUID, "lastOnline", Long.toString(System.currentTimeMillis()));
                }
                pipe.sync();
            }
        }
        pidgin.sendPacket(new NetworkStatusPacket(proxy, false));
        instance.getLogger().info("Removed current proxy instance in " + (System.currentTimeMillis() - start) + "ms");
    }

    private void updatePlayerCounts() {
        int online = 0;

        try(Jedis jedis = getConnection()) {
            Map<String, String> proxies = jedis.hgetAll("proxies");
            for(Map.Entry<String, String> entry : proxies.entrySet()) {
                final String proxy = entry.getKey();
                if(System.currentTimeMillis() - Long.parseLong(entry.getValue()) > TimeUnit.SECONDS.toMillis(15)) {
                    removeProxyInstance(proxy);
                    instance.getLogger().warn("No heartbeat from " + proxy + " in 15 seconds, removing proxy.");
                    continue;
                }

                if(System.currentTimeMillis() - Long.parseLong(entry.getValue()) > TimeUnit.SECONDS.toMillis(5)) {
                    instance.getLogger().warn("No heartbeat from " + proxy + " in 5 seconds, ignoring players.");
                    knownProxies.remove(proxy.toLowerCase());
                    continue;
                }

                knownProxies.add(proxy.toLowerCase());
                online += jedis.hlen("proxy:" + proxy + ":onlines");
            }

            jedis.hset("proxies", proxyName, Long.toString(System.currentTimeMillis()));
        }

        onlinePlayers.set(online);
    }

    private void fixPlayerProxyInconsistency() {
        long start = System.currentTimeMillis();
        int count = 0;

        try(Jedis jedis = getConnection()) {
            final Map<String, String> onlines = jedis.hgetAll("proxy:" + proxyName + ":onlines");

            try(final Pipeline pipe = jedis.pipelined()) {
                for(Map.Entry<String, String> entry : onlines.entrySet()) {
                    final UUID uuid = UUID.fromString(entry.getKey());
                    if(!instance.getProxy().getPlayer(uuid).isPresent()) {
                        for(RegisteredServer server : instance.getProxy().getAllServers()) {
                            pipe.srem("server:" + server.getServerInfo().getName(), entry.getValue());
                        }

                        pipe.hdel("proxy:" + proxyName + ":onlines", uuid.toString());
                        pipe.hset("player:" + uuid, "lastOnline", Long.toString(System.currentTimeMillis()));

                        instance.getLogger().warn("Removing " + entry.getKey() + " because it's stored in redis but not online");
                        count++;
                    }
                }
                pipe.sync();
            }
        }
        if(count == 0) return;

        instance.getLogger().warn("Removed " + count + " players due to inconsistency between in-game and redis in " + (System.currentTimeMillis() - start) + "ms");
    }

    private void checkClockDifference() {
        try(Jedis jedis = getConnection()) {
            final Map<String, String> proxies = jedis.hgetAll("proxies");
            for(String proxy : proxies.keySet()) {
                long heartbeat = Long.parseLong(proxies.get(proxy));
                final long diff = Math.abs(System.currentTimeMillis() - heartbeat);
                if(diff > TimeUnit.SECONDS.toMillis(4)) {
                    instance.getLogger().warn("Time difference of " + diff + "ms exceeds the 4s margin between proxies");
                }
            }
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

        try(final Jedis jedis = getConnection()) {
            final Transaction tx = jedis.multi();

            tx.hmset("proxy:" + proxyName + ":onlines", proxyData);
            tx.hmset("player:" + uuid, playerData);

            tx.exec();
        }
    }

    /**
     * This method sets the server where a Player is
     *
     * @param uuid      The Player's UUID
     * @param username  The Player's username
     * @param newServer The server
     */
    public void setPlayerServer(UUID uuid, String username, String newServer) {
        try(final Jedis jedis = getConnection(); final Pipeline pipe = jedis.pipelined()) {
            for(RegisteredServer server : instance.getProxy().getAllServers()) {
                pipe.srem("server:" + server.getServerInfo().getName(), username);
            }

            pipe.hset("player:" + uuid, "server", newServer);
            pipe.sadd("server:" + newServer, username);

            pipe.sync();
        }
    }

    /**
     * This method gets the PlayerData of a Player
     *
     * @param uuid The UUID of the Player
     * @return The PlayerData, null if not found
     */
    public PlayerData getPlayer(UUID uuid) {
        try(final Jedis jedis = getConnection()) {
            Map<String, String> data = jedis.hgetAll("player:" + uuid);
            if(data == null || data.isEmpty())
                return null;

            return new PlayerData(
                    uuid,
                    data.getOrDefault("username", ""),
                    Long.parseLong(data.get("lastOnline")),
                    data.getOrDefault("ip", null),
                    data.getOrDefault("proxy", null),
                    data.getOrDefault("server", null)
            );
        }
    }

    /**
     * This method cleans all the info of a Player
     *
     * @param uuid The UUID of the Player
     */
    public void cleanPlayer(UUID uuid, String username) {
        try(final Jedis jedis = getConnection(); final Pipeline pipe = jedis.pipelined()) {
            for(RegisteredServer server : instance.getProxy().getAllServers()) {
                pipe.srem("server:" + server.getServerInfo().getName(), username);
            }

            pipe.hdel("proxy:" + proxyName + ":onlines", uuid.toString());
            pipe.hset("player:" + uuid, "lastOnline", Long.toString(System.currentTimeMillis()));

            pipe.sync();
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
            if(s == null) {
                try {
                    final Resolver.Result result = instance.getResolverManager().resolveUsername(player);
                    if(result != null) {
                        return result.getUniqueId();
                    }
                } catch(Exception ignore) {
                }
                return null;
            }

            return UUID.fromString(s);
        }
    }

    /**
     * This method gets all the online players in <strong>all the online proxies</strong>
     *
     * @return A Set of Strings with all the Players from all the proxies, null if proxy not found
     */
    public Set<String> getUsernamesOnline() {
        List<Response<List<String>>> results = new ArrayList<>(knownProxies.size());
        try(final Jedis jedis = getConnection(); final Pipeline pipe = jedis.pipelined()) {
            for(String proxy : knownProxies) {
                results.add(pipe.hvals("proxy:" + proxy + ":onlines"));
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
    public Set<String> getServerUsernames(String server) {
        try(Jedis jedis = getConnection()) {
            return jedis.smembers("server:" + server);
        }
    }

    /**
     * This method gets the player count in a server
     *
     * @param server The proxy name
     * @return The player count, null if server not found
     */
    public int getServerCount(String server) {
        try(Jedis jedis = getConnection()) {
            return Math.toIntExact(jedis.scard("server:" + server));
        }
    }

    public void setPlayerResult(String username, Resolver.Result result) {
        try(Jedis jedis = getConnection()) {
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("uuid", result.getUniqueId().toString());
            data.put("premium", String.valueOf(result.isPremium()));
            data.put("source", result.getSource());

            jedis.hset("resolver:" + username.toLowerCase(), data);
            jedis.expire("resolver:" + username.toLowerCase(), CACHE_RESOLVER);

            jedis.setex("playeruuid:" + username.toLowerCase(), CACHE_UUID, result.getUniqueId().toString());
        }
    }

    public Resolver.Result getPlayerResult(String username) {
        try(Jedis jedis = getConnection()) {
            final Map<String, String> result = jedis.hgetAll("resolver:" + username.toLowerCase());

            if(result == null || result.isEmpty()) {
                return null;
            }

            return new Resolver.Result(result.get("username"), UUID.fromString(result.get("uuid")),
                    Boolean.parseBoolean(result.get("premium")), result.get("source"));
        }
    }

    public void clearCache(String username) {
        try(Jedis jedis = getConnection()) {
            jedis.del("resolver:" + username.toLowerCase());
            jedis.del("playeruuid:" + username.toLowerCase());
        }
    }

    private Jedis getConnection() {
        return pool.getResource();
    }

}
