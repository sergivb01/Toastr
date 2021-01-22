package services.vortex.toastr.backend.mysql;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.proxy.Player;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.utils.MyMonitorThread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.*;

public class BackendStorage {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final int MIN_IDLE = 8;
    private static final long NAMECHANGE_DELAY = TimeUnit.DAYS.toMillis(37);

    private final ThreadPoolExecutor executor;
    private final MyMonitorThread monitor;
    private final HikariDataSource hikari;

    public BackendStorage(BackendCredentials credentials) {
        // Number of threads = Number of Available Cores * (1 + Wait time / Service time)
        int numThreads = Runtime.getRuntime().availableProcessors() * (1 + 10 / 5);
        instance.getLogger().info("Setting a max pool of " + numThreads + " for the executor");

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Toastr Storage - %1$d")
                .setDaemon(true).build();
        this.executor = new ThreadPoolExecutor(MIN_IDLE, numThreads, 15L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
        this.monitor = new MyMonitorThread(executor, 15 * 60);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

        executor.allowCoreThreadTimeOut(true);

        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(MIN_IDLE);
        config.setPoolName("Toastr-Hikari");
        config.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");

        config.addDataSourceProperty("serverName", credentials.getHostname());
        config.addDataSourceProperty("port", credentials.getPort());
        config.addDataSourceProperty("databaseName", credentials.getDatabase());
        config.addDataSourceProperty("user", credentials.getUsername());
        config.addDataSourceProperty("password", credentials.getPassword());

        config.addDataSourceProperty("paranoid", true);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("maintainTimeStats", false);

        this.hikari = new HikariDataSource(config);
    }

    /**
     * This method shutdowns the executor pool, monitor thread and HikariCP
     */
    @SneakyThrows
    public void shutdown() {
        monitor.shutdown();

        instance.getLogger().info("ExecutorService is being shutdown, awaiting tasks to finish");
        executor.shutdown();
        try {
            // Velocity has a timeout to disable all plugins of 10 seconds
            if(!executor.awaitTermination(8, TimeUnit.SECONDS)) {
                instance.getLogger().warn("Timed out while waiting ExecutorService to finish");
                executor.shutdownNow();
            }
        } catch(InterruptedException e) {
            executor.shutdownNow();
        }

        this.hikari.close();
    }

    private Profile createProfileFromRS(final ResultSet rs) throws SQLException {
        return new Profile(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("player_name"),
                Profile.AccountType.valueOf(rs.getString("account_type")),
                rs.getString("first_address"),
                rs.getString("last_address"),
                rs.getTimestamp("first_login"),
                rs.getTimestamp("last_login"),
                rs.getString("password"),
                rs.getString("salt"),
                false
        );
    }

    /**
     * This method checks if a player with different namecase exists
     *
     * @param player The player
     * @return CompletableFuture<Boolean> that can return a SQLException
     */
    public CompletableFuture<Profile.CheckAccountResult> checkAccounts(final Player player) {
        CompletableFuture<Profile.CheckAccountResult> future = new CompletableFuture<>();
        final long start = System.currentTimeMillis();

        executor.submit(() -> {
            try(Connection connection = this.hikari.getConnection();
                final PreparedStatement query = connection.prepareStatement(SQLQueries.CHECK_NAMECASE.getQuery())) {
                query.setString(1, player.getUsername());
                query.setQueryTimeout(3);

                try(final ResultSet rs = query.executeQuery()) {
                    // player doesn't exist in database
                    if(!rs.next() || player.isOnlineMode()) {
                        future.complete(Profile.CheckAccountResult.ALLOWED);
                        return;
                    }

                    final Profile profile = createProfileFromRS(rs);

                    // cracked player trying to login with different name-case - block
                    if(!profile.getUsername().equals(player.getUsername())) {
                        future.complete(Profile.CheckAccountResult.DIFFERENT_NAMECASE);
                        instance.getLogger().info("[database] [CheckAccounts] " + player.getUsername() + "(" + player.getUniqueId() + ") took " + (System.currentTimeMillis() - start) + "ms");
                        return;
                    }

                    // cracked player logged in with nickname of an premium account (< 37d ?)
                    long elapsedSinceLast = System.currentTimeMillis() - profile.getLastLogin().getTime();
                    if(profile.getAccountType().equals(Profile.AccountType.PREMIUM) && elapsedSinceLast < NAMECHANGE_DELAY) {
                        // premium account is still premium, block
                        future.complete(Profile.CheckAccountResult.OLD_PREMIUM);
                        instance.getLogger().info("[database] [CheckAccounts] " + player.getUsername() + "(" + player.getUniqueId() + ") took " + (System.currentTimeMillis() - start) + "ms");
                        return;
                    }

                    // premium account is no longer premium, allow, or other cases
                    future.complete(Profile.CheckAccountResult.ALLOWED);

                    instance.getLogger().info("[database] [CheckAccounts] " + player.getUsername() + "(" + player.getUniqueId() + ") took " + (System.currentTimeMillis() - start) + "ms");
                }
            } catch(SQLException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /**
     * This method gets a player Profile from the database
     *
     * @param playerUUID The UUID from the player. Can be an offline UUID
     * @return CompletableFuture<Profile> that can return a SQLException
     */
    public CompletableFuture<Profile> getProfile(UUID playerUUID) {
        CompletableFuture<Profile> future = new CompletableFuture<>();
        final long start = System.currentTimeMillis();

        executor.submit(() -> {
            try(Connection connection = this.hikari.getConnection();
                final PreparedStatement query = connection.prepareStatement(SQLQueries.SELECT_PROFILE_BY_UUID.getQuery())) {
                query.setString(1, playerUUID.toString());
                query.setQueryTimeout(3);

                try(final ResultSet rs = query.executeQuery()) {
                    if(!rs.next()) {
                        future.complete(null);
                        return;
                    }

                    future.complete(createProfileFromRS(rs));
                    instance.getLogger().info("[database] [GetProfile] " + playerUUID.toString() + " took " + (System.currentTimeMillis() - start) + "ms");
                }
            } catch(SQLException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /**
     * This method saves a player Profile to the database
     *
     * @param profile The profile that needs to be stored.
     * @return CompletableFuture<Boolean> that can return a SQLException. Returns true if the player never played before
     */
    public CompletableFuture<Boolean> saveProfile(final Profile profile) {
        final long start = System.currentTimeMillis();
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        executor.submit(() -> {
            try(Connection connection = this.hikari.getConnection()) {
                // TODO: change order: first update then insert (we have more UPDATEs than INSERTs)
                try(final PreparedStatement query = connection.prepareStatement(SQLQueries.UPDATE_PROFILE_BY_UUID.getQuery())) {
                    query.setString(1, profile.getUsername());
                    query.setString(2, profile.getUsername());
                    query.setString(3, profile.getLastIP());
                    query.setTimestamp(4, profile.getLastLogin());
                    query.setString(5, profile.getPassword());
                    query.setString(6, profile.getSalt());
                    query.setString(7, profile.getUniqueId().toString());

                    query.setQueryTimeout(3);

                    if(query.executeUpdate() != 0) {
                        future.complete(false);
                        instance.getLogger().info("[database] [SaveProfile] " + profile.getUniqueId() + " took " + (System.currentTimeMillis() - start) + "ms");
                        return;
                    }
                }

                try(final PreparedStatement query = connection.prepareStatement(SQLQueries.INSERT_PROFILE.getQuery())) {
                    query.setString(1, profile.getUniqueId().toString());
                    query.setString(2, profile.getUsername());
                    query.setString(3, profile.getUsername());
                    query.setString(4, profile.getAccountType().toString());
                    query.setString(5, profile.getFirstIP());
                    query.setString(6, profile.getLastIP());
                    query.setTimestamp(7, profile.getFirstLogin());
                    query.setTimestamp(8, profile.getLastLogin());
                    query.setString(9, profile.getPassword());
                    query.setString(10, profile.getSalt());

                    query.setQueryTimeout(3);

                    if(query.executeUpdate() == 1) {
                        future.complete(true);
                        instance.getLogger().info("[database] [InsertProfile] " + profile.getUniqueId() + " took " + (System.currentTimeMillis() - start) + "ms");
                    }
                }
            } catch(SQLException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

}
