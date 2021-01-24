package services.vortex.toastr.backend.mysql;

import lombok.NonNull;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.utils.ResourceReader;

public enum SQLQueries {
    CHECK_NAMECASE,
    INSERT_PROFILE,
    SELECT_PROFILE_BY_UUID,
    UNREGISTER_BY_USERNAME,
    UPDATE_PROFILE_BY_UUID;

    private static final String PATH = "sql/";
    private final @NonNull String query;

    SQLQueries() {
        ToastrPlugin.getInstance().getLogger().info("[MySQL] Loading query " + this.toString() + "(" + PATH + this.toString().toLowerCase().replace("_", "-") + ".sql)");
        this.query = ResourceReader.readResource(PATH + this.toString().toLowerCase().replace("_", "-") + ".sql");
    }

    /**
     * Get the SQL query this wraps.
     *
     * @return The query for this enumeration.
     */
    public @NonNull String getQuery() {
        return this.query;
    }

}
