package services.vortex.toastr.backend;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.tascalate.concurrent.CompletableTask;
import net.tascalate.concurrent.Promise;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;

import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BackendStorage {
    protected static final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("Toastr Storage - %1$d")
            .setDaemon(true)
            .build());
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private final HikariDataSource hikari;

    public BackendStorage(BackendCredentials credentials) {
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

    public Promise<Profile> getPlayerProfile(UUID playerUUID) {
        return CompletableTask.submit(() -> {
            try(Connection connection = this.hikari.getConnection()) {
/*          final PreparedStatement preparedStatement = connection.prepareStatement("");
            preparedStatement.setQueryTimeout(5);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Coins(UUID varchar(36), name VARCHAR(16), COINS int)");
 */
                Thread.sleep(3000L);
                return new Profile(playerUUID, "test123");
            }
        }, executor).orTimeout(3500, TimeUnit.MILLISECONDS);
    }

}
