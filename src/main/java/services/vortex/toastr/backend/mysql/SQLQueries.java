package services.vortex.toastr.backend.mysql;

import lombok.NonNull;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.utils.ResourceReader;

public enum SQLQueries {
    SELECT_PROFILE_BY_UUID,
    INSERT_PROFILE,
    UPDATE_PROFILE_BY_UUID;

    private static final String PATH = "sql/";
    private final @NonNull String query;

    SQLQueries() {
        ToastrPlugin.getInstance().getLogger().info("Loading query " + this.toString() + "(" + PATH + this.toString().toLowerCase().replace("_", "-") + ".sql)");
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
