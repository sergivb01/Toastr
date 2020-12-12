package services.vortex.toastr.backend;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

public class BackendStorage {
    //    protected static final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
//            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("Toastr Storage - %1$d")
//            .setDaemon(true)
//            .build());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private final HikariDataSource hikari;

    public BackendStorage(BackendCredentials credentials) {
//        executor.allowCoreThreadTimeOut(true);

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

    public static <T> CompletableFuture<T> within(CompletableFuture<T> future, Duration duration) {
        final CompletableFuture<T> timeout = failAfter(duration);
        return future.applyToEither(timeout, Function.identity());
    }

    public static <T> CompletableFuture<T> failAfter(Duration duration) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        scheduler.schedule(() -> {
            final TimeoutException ex = new TimeoutException("Timeout after " + duration);
            return promise.completeExceptionally(ex);
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
        return promise;
    }

    public CompletableFuture<Profile> getPlayerProfile(UUID playerUUID) {
        CompletableFuture<Profile> res = new CompletableFuture<>();

        instance.getProxy().getScheduler().buildTask(instance, () -> {
            try(Connection connection = this.hikari.getConnection()) {

/*          final PreparedStatement preparedStatement = connection.prepareStatement("");
            preparedStatement.setQueryTimeout(5);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Coins(UUID varchar(36), name VARCHAR(16), COINS int)");
 */
                Thread.sleep(3000L);

                res.complete(new Profile(playerUUID, "test123"));

            } catch(SQLException | InterruptedException e) {
                res.completeExceptionally(e);
            }
        }).schedule();

        return within(res, Duration.ofSeconds(5));
    }

}
