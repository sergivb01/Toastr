package services.vortex.toastr.backend.mysql;

import com.velocitypowered.api.proxy.Player;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.BackendCredentials;
import services.vortex.toastr.profile.Profile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BackendStorage {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final int MIN_IDLE = 8, MAX_CONNS = 14;
    private static final long NAMECHANGE_DELAY = TimeUnit.DAYS.toMillis(37);

    private final HikariDataSource hikari;

    public BackendStorage(final BackendCredentials credentials) {
        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(MIN_IDLE);
        config.setMaximumPoolSize(MAX_CONNS);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(3)); // max time to get a connection before Exception
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
     * This method shutdowns HikariCP
     */
    @SneakyThrows
    public void shutdown() {
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
    public Profile.CheckAccountResult checkAccounts(final Player player) throws Exception {
        // premium user has the privilege to use the account
        if(player.isOnlineMode()) {
            return Profile.CheckAccountResult.ALLOWED;
        }
        final long start = System.currentTimeMillis();

        try(Connection connection = this.hikari.getConnection();
            final PreparedStatement query = connection.prepareStatement(SQLQueries.CHECK_NAMECASE.getQuery())) {
            query.setString(1, player.getUsername());
            query.setQueryTimeout(1);

            try(final ResultSet rs = query.executeQuery()) {
                while(rs.next()) {
                    final Profile profile = createProfileFromRS(rs);

                    // cracked player trying to login with different name-case - block
                    if(!profile.getUsername().equals(player.getUsername())) {
                        return Profile.CheckAccountResult.DIFFERENT_NAMECASE;
                    }

                    // cracked player logged in with nickname of an premium account (< 37d ?)
                    long elapsedSinceLast = System.currentTimeMillis() - profile.getLastLogin().getTime();
                    if(profile.getAccountType().equals(Profile.AccountType.PREMIUM) && elapsedSinceLast < NAMECHANGE_DELAY) {
                        // premium account is still premium, block
                        return Profile.CheckAccountResult.OLD_PREMIUM;
                    }
                }

                // player doesn't exist in database, old premium account is no longer premium...
                return Profile.CheckAccountResult.ALLOWED;
            }
        } finally {
            instance.getLogger().info("[database] [CheckAccounts] " + player.getUsername() + "(" + player.getUniqueId() + ") took " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    /**
     * This method gets a player Profile from the database
     *
     * @param playerUUID The UUID from the player. Can be an offline UUID
     * @return CompletableFuture<Profile> that can return a SQLException
     */
    public Profile getProfile(UUID playerUUID) throws Exception {
        final long start = System.currentTimeMillis();

        try(Connection connection = this.hikari.getConnection();
            final PreparedStatement query = connection.prepareStatement(SQLQueries.SELECT_PROFILE_BY_UUID.getQuery())) {
            query.setString(1, playerUUID.toString());
            query.setQueryTimeout(1);

            try(final ResultSet rs = query.executeQuery()) {
                if(!rs.next()) {
                    return null;
                }

                return createProfileFromRS(rs);
            }
        } finally {
            instance.getLogger().info("[database] [GetProfile] " + playerUUID.toString() + " took " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    // TODO: documentation
    public void unregister(UUID playerUUID) throws Exception {
        final long start = System.currentTimeMillis();

        try(Connection connection = this.hikari.getConnection();
            final PreparedStatement query = connection.prepareStatement(SQLQueries.UNREGISTER_BY_UUID.getQuery())) {
            query.setString(1, playerUUID.toString());
            query.setQueryTimeout(1);

            query.execute();
        } finally {
            instance.getLogger().info("[database] [Unregister] " + playerUUID.toString() + " took " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    /**
     * This method saves a player Profile to the database
     *
     * @param profile The profile that needs to be stored.
     * @return CompletableFuture<Boolean> that can return a SQLException. Returns true if the player never played before
     */
    public boolean saveProfile(final Profile profile) throws Exception {
        final long start = System.currentTimeMillis();

        try(Connection connection = this.hikari.getConnection()) {
            try(final PreparedStatement query = connection.prepareStatement(SQLQueries.UPDATE_PROFILE_BY_UUID.getQuery())) {
                query.setString(1, profile.getUsername());
                query.setString(2, profile.getLastIP());
                query.setTimestamp(3, profile.getLastLogin());
                query.setString(4, profile.getPassword());
                query.setString(5, profile.getSalt());
                query.setString(6, profile.getUniqueId().toString());

                query.setQueryTimeout(1);

                if(query.executeUpdate() == 1) {
                    return false;
                }
            }

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

                query.setQueryTimeout(1);

                query.execute();

                return true;
            }
        } finally {
            instance.getLogger().info("[database] [Insert/SaveProfile] " + profile.getUniqueId() + " took " + (System.currentTimeMillis() - start) + "ms");
        }
    }

}