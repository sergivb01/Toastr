package services.vortex.toastr.backend.mysql;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

    private final ThreadPoolExecutor executor;
    private final MyMonitorThread monitor;
    private final HikariDataSource hikari;

    public BackendStorage(BackendCredentials credentials) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Toastr Storage - %1$d")
                .setDaemon(true).build();
        this.executor = new ThreadPoolExecutor(MIN_IDLE, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
        this.monitor = new MyMonitorThread(executor, 60);
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
            if(!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                instance.getLogger().warn("timed out while waiting ExecutorService to finish");
                executor.shutdownNow();
            }
        } catch(InterruptedException e) {
            executor.shutdownNow();
        }

        this.hikari.close();
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

                final ResultSet rs = query.executeQuery();
                if(!rs.next()) {
                    future.complete(null);
                    return;
                }

                future.complete(new Profile(
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
                ));
                instance.getLogger().info("[DATABASE] [GetProfile] " + playerUUID.toString() + " took " + (System.currentTimeMillis() - start) + "ms");
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

                try(final PreparedStatement query = connection.prepareStatement(SQLQueries.INSERT_PROFILE.getQuery())) {
                    query.setString(1, profile.getUniqueId().toString());
                    query.setString(2, profile.getUsername());
                    query.setString(3, profile.getAccountType().toString());
                    query.setString(4, profile.getFirstIP());
                    query.setString(5, profile.getLastIP());
                    query.setTimestamp(6, profile.getFirstLogin());
                    query.setTimestamp(7, profile.getLastLogin());
                    query.setString(8, profile.getPassword());
                    query.setString(9, profile.getSalt());

                    query.setQueryTimeout(3);

                    if(query.executeUpdate() == 1) {
                        future.complete(true);
                        instance.getLogger().info("[DATABASE] [InsertProfile] " + profile.getUniqueId() + " took " + (System.currentTimeMillis() - start) + "ms");
                        return;
                    }
                }

                try(final PreparedStatement query = connection.prepareStatement(SQLQueries.UPDATE_PROFILE_BY_UUID.getQuery())) {
                    query.setString(1, profile.getUsername());
                    query.setString(2, profile.getLastIP());
                    query.setTimestamp(3, profile.getLastLogin());
                    query.setString(4, profile.getPassword());
                    query.setString(5, profile.getSalt());
                    query.setString(6, profile.getUniqueId().toString());

                    query.setQueryTimeout(3);

                    query.execute();

                    future.complete(false);
                    instance.getLogger().info("[DATABASE] [SaveProfile] " + profile.getUniqueId() + " took " + (System.currentTimeMillis() - start) + "ms");
                }

            } catch(SQLException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

}
