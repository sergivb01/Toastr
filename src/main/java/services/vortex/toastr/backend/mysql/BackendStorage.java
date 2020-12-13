package services.vortex.toastr.backend.mysql;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.utils.MyMonitorThread;
import services.vortex.toastr.utils.RejectedExecutionHandlerImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.*;

public class BackendStorage {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private final ThreadPoolExecutor executor;
    private final MyMonitorThread monitor;
    private final HikariDataSource hikari;

    public BackendStorage(BackendCredentials credentials) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Toastr Storage - %1$d")
                .setDaemon(true).build();
        this.executor = new ThreadPoolExecutor(16, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory, new RejectedExecutionHandlerImpl());
        this.monitor = new MyMonitorThread(executor, 60);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

        executor.allowCoreThreadTimeOut(true);

        HikariConfig config = new HikariConfig();
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
        instance.getLogger().info(
                String.format("[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
                        this.executor.getPoolSize(),
                        this.executor.getCorePoolSize(),
                        this.executor.getActiveCount(),
                        this.executor.getCompletedTaskCount(),
                        this.executor.getTaskCount(),
                        this.executor.isShutdown(),
                        this.executor.isTerminated()));
        while(executor.getCompletedTaskCount() - executor.getTaskCount() > 0) {
            Thread.sleep(250);
            instance.getLogger().warn("sleeping...");
        }
        executor.shutdown();
        monitor.shutdown();

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

        executor.submit(() -> {
            try(Connection connection = this.hikari.getConnection();
                final PreparedStatement query = connection.prepareStatement("SELECT * FROM playerdata WHERE uuid = ?")) {
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
    public CompletableFuture<Boolean> savePlayer(final Profile profile) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        executor.submit(() -> {
            try(Connection connection = this.hikari.getConnection()) {

                try(final PreparedStatement query = connection.prepareStatement("INSERT IGNORE INTO playerdata VALUES (?, ?, ?, INET_ATON(?), INET_ATON(?), ?, ?, ?, ?)")) {
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
                        return;
                    }
                }

                try(final PreparedStatement query = connection.prepareStatement("UPDATE playerdata SET player_name = ?, last_address = INET_ATON(?), last_login = ?, last_login_at = ?, password = ?, salt = ? WHERE uuid = ?")) {
                    query.setString(1, profile.getUsername());
                    query.setString(2, profile.getLastIP());
                    query.setTimestamp(3, profile.getLastLogin());
                    query.setString(4, profile.getPassword());
                    query.setString(5, profile.getSalt());
                    query.setString(6, profile.getUniqueId().toString());

                    query.setQueryTimeout(3);

                    future.complete(false);
                }

            } catch(SQLException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

}
