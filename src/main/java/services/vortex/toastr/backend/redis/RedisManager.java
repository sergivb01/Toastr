package services.vortex.toastr.backend.redis;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisNoScriptException;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.BackendCredentials;
import services.vortex.toastr.backend.packets.Pidgin;
import services.vortex.toastr.backend.packets.types.*;
import services.vortex.toastr.listeners.NetworkListener;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.resolver.Resolver;
import services.vortex.toastr.utils.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RedisManager {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    private static final int RESOLVER_TTL = (int) TimeUnit.DAYS.toSeconds(2);
    private static final int UUID_TTL = (int) TimeUnit.DAYS.toSeconds(7);
    private static final int PROFILE_TTL = (int) TimeUnit.DAYS.toSeconds(7);

    private final JedisPool pool;
    private final ScheduledTask updateTask, consistencyCheckTask;

    @Getter
    private final AtomicInteger onlinePlayers = new AtomicInteger(0);
    @Getter
    private final String proxyName;
    @Getter
    private final Pidgin pidgin;

    public RedisManager(final BackendCredentials credentials) {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(10);
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(3)); // max time to get a connection before Exception

        if(StringUtils.isNullOrEmpty(credentials.getPassword())) {
            pool = new JedisPool(poolConfig, credentials.getHostname(), credentials.getPort());
        } else {
            pool = new JedisPool(poolConfig, credentials.getHostname(), credentials.getPort(), 2000, credentials.getPassword());
        }

        proxyName = instance.getConfig().getString("proxy-name");

        pidgin = new Pidgin("toastr", pool);
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

        final Scheduler scheduler = instance.getProxy().getScheduler();

        scheduler.buildTask(instance, () -> pidgin.sendPacket(new NetworkStatusPacket(proxyName, true)))
                .delay(1, TimeUnit.SECONDS).schedule();

        consistencyCheckTask = scheduler.buildTask(instance, this::checkInconsistency)
                .delay(10, TimeUnit.SECONDS).repeat(1, TimeUnit.MINUTES).schedule();

        updateTask = scheduler.buildTask(instance, this::updatePlayerCounts)
                .repeat(1, TimeUnit.SECONDS).schedule();
    }

    /**
     * Shutdowns cancels all the tasks
     * and closes the connection pool
     */
    public void shutdown() {
        removeProxyInstance(proxyName);

        consistencyCheckTask.cancel();
        updateTask.cancel();

        pool.close();
    }

    private void removeProxyInstance(String proxy) {
        final long start = System.currentTimeMillis();

        executeScript(LuaScripts.REMOVE_PROXY, Collections.singletonList(proxy), null);
        pidgin.sendPacket(new NetworkStatusPacket(proxy, false));

        instance.getLogger().info("Removed proxy {} instance in {}ms", proxy, System.currentTimeMillis() - start);
    }

    private void updatePlayerCounts() {
        onlinePlayers.set(((Long) executeScript(LuaScripts.GET_PLAYER_COUNT, Collections.singletonList(proxyName), null)).intValue());
    }

    // TODO: migrate to Lua script
    private void checkInconsistency() {
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

    /**
     * Loads a redis script into redis
     *
     * @param data the contents of the script
     * @return hash of the loaded script
     */
    public String createScript(String data) {
        try(final Jedis jedis = getConnection()) {
            return jedis.scriptLoad(data);
        }
    }

    /**
     * Executes a redis script
     *
     * @param script script to be executed
     * @param keys   keys for the script
     * @param args   arguments for the script
     */
    public Object executeScript(LuaScripts script, List<String> keys, List<String> args) {
        if(keys == null) keys = ImmutableList.of();

        if(args == null) args = ImmutableList.of();

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
            jedis.hmset("proxy:" + proxyName + ":onlines", proxyData);
            jedis.hmset("player:" + uuid, playerData);
        }
    }

    /**
     * This method sets the server where a Player is
     *
     * @param uuid      The Player's UUID
     * @param username  The Player's username
     * @param newServer The server
     */
    // TODO: migrate to lua
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
    // TODO: migrate to lua
    public void cleanPlayer(UUID uuid, String username) {
        try(final Jedis jedis = getConnection(); final Pipeline pipe = jedis.pipelined()) {
            for(RegisteredServer server : instance.getProxy().getAllServers()) {
                pipe.srem("server:" + server.getServerInfo().getName(), username);
            }

            pipe.hdel("proxy:" + proxyName + ":onlines", uuid.toString());
            pipe.hset("player:" + uuid, "lastOnline", Long.toString(System.currentTimeMillis()));
            pipe.expire("player:" + uuid, PROFILE_TTL);

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
        try(final Jedis jedis = getConnection()) {
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
        return new HashSet<>((List<String>) executeScript(LuaScripts.GET_ONLINE_USERNAMES, null, null));
    }

    /**
     * This method gets the player count in a proxy
     *
     * @param proxy The proxy name
     * @return The player count, null if proxy not found
     */
    public Long getProxyCount(String proxy) {
        try(final Jedis jedis = getConnection()) {
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
        try(final Jedis jedis = getConnection()) {
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
        try(final Jedis jedis = getConnection()) {
            return Math.toIntExact(jedis.scard("server:" + server));
        }
    }

    /**
     * Sets the ResolverResult of a username
     *
     * @param username The player username
     * @param result   The resolver result
     */
    public void setPlayerResult(String username, Resolver.Result result) {
        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("uuid", result.getUniqueId().toString());
        data.put("premium", String.valueOf(result.isPremium()));
        data.put("source", result.getSource());

        try(final Jedis jedis = getConnection()) {
            jedis.hset("resolver:" + username.toLowerCase(), data);
            jedis.expire("resolver:" + username.toLowerCase(), RESOLVER_TTL);

            jedis.setex("playeruuid:" + username.toLowerCase(), UUID_TTL, result.getUniqueId().toString());
        }
    }

    public Resolver.Result getPlayerResult(String username) {
        try(final Jedis jedis = getConnection()) {
            final Map<String, String> result = jedis.hgetAll("resolver:" + username.toLowerCase());

            if(result == null || result.isEmpty()) {
                return null;
            }

            return new Resolver.Result(result.get("username"), UUID.fromString(result.get("uuid")),
                    Boolean.parseBoolean(result.get("premium")), result.get("source"));
        }
    }

    public void clearCache(String username) {
        try(final Jedis jedis = getConnection()) {
            jedis.del("resolver:" + username.toLowerCase());
            jedis.del("playeruuid:" + username.toLowerCase());
        }
    }

    private Jedis getConnection() {
        return pool.getResource();
    }

}
