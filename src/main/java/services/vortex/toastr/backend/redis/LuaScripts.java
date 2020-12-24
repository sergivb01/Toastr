package services.vortex.toastr.backend.redis;

import lombok.NonNull;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.utils.ResourceReader;

public enum LuaScripts {
    SERVER_TO_PLAYERS;

    private static final String PATH = "lua/";
    private final @NonNull String script;
    private final @NonNull String hash;

    LuaScripts() {
        final ToastrPlugin instance = ToastrPlugin.getInstance();

        this.script = ResourceReader.readResource(PATH + this.toString().toLowerCase().replace("_", "-") + ".lua");
        this.hash = instance.getRedisManager().createScript(script);

        instance.getLogger().info("Loading query " + this.toString() + "(" + PATH + this.toString().toLowerCase().replace("_", "-") + ".lua)");
    }

    /**
     * Get the SHA1 this wraps.
     *
     * @return The hash for this lua script
     */
    public @NonNull String getHash() {
        return this.hash;
    }


    /**
     * Get the script this wraps.
     *
     * @return The script contents for this lua script
     */
    public @NonNull String getScript() {
        return this.script;
    }

}
