package services.vortex.toastr.backend;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.utils.MyMonitorThread;
import services.vortex.toastr.utils.RejectedExecutionHandlerImpl;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;

public class BackendStorage {
    private final ThreadPoolExecutor executor;
    private final MyMonitorThread monitor;
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private final HikariDataSource hikari;

    public BackendStorage(BackendCredentials credentials) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Toastr Storage - %1$d")
                .setDaemon(true).build();
        this.executor = new ThreadPoolExecutor(16, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory, new RejectedExecutionHandlerImpl());
        this.monitor = new MyMonitorThread(executor, 15);
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

    public void shutdown() {
        executor.shutdown();
        monitor.shutdown();

        this.hikari.close();
    }

    public CompletableFuture<Profile> getProfile(UUID playerUUID) {
        CompletableFuture<Profile> future = new CompletableFuture<>();

        executor.submit(() -> {
            try(Connection connection = this.hikari.getConnection();
                final PreparedStatement query = connection.prepareStatement("SELECT uuid, player_name, account_type, INET_NTOA(first_address) AS first_address, INET_NTOA(last_address) AS last_address, first_login, last_login, last_login_at, password FROM playerdata WHERE uuid = ?")) {
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
                        rs.getTimestamp("first_login").toInstant().toEpochMilli(),
                        rs.getTimestamp("last_login").toInstant().toEpochMilli(),
                        rs.getString("last_login_at"),
                        rs.getString("password"),
                        false
                ));
            } catch(SQLException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    public CompletableFuture<Boolean> savePlayer(final Profile profile) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        executor.submit(() -> {
            try(Connection connection = this.hikari.getConnection();
                final PreparedStatement query = connection.prepareStatement("INSERT INTO playerdata SET uuid = ?, player_name = ?, account_type = ?, first_address = INET_ATON(?), last_address = INET_ATON(?), last_login = ?, last_login_at = ?, password = ?\n" +
                        "    ON DUPLICATE KEY UPDATE player_name = ?, account_type = ?, last_address = INET_ATON(?), last_login = ?, last_login_at = ?, password = ?")) {
                query.setString(1, profile.getUniqueId().toString());
                query.setString(2, profile.getUsername());
                query.setString(3, profile.getAccountType().toString());
                query.setString(4, profile.getFirstIP());
                query.setString(5, profile.getLastIP());
                query.setTimestamp(6, new Timestamp(profile.getLastLogin()));
                query.setString(7, profile.getLastServer());
                query.setString(8, profile.getPassword());
                //////////
                query.setString(9, profile.getUsername());
                query.setString(10, profile.getAccountType().toString());
                query.setString(11, profile.getLastIP());
                query.setTimestamp(12, new Timestamp(profile.getLastLogin()));
                query.setString(13, profile.getLastServer());
                query.setString(14, profile.getPassword());

                query.setQueryTimeout(3);

                future.complete(query.executeUpdate() == 1);
            } catch(SQLException ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

}
