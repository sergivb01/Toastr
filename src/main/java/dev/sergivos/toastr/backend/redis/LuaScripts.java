package dev.sergivos.toastr.backend.redis;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.utils.ResourceReader;
import lombok.NonNull;

public enum LuaScripts {
    GET_ONLINE_USERNAMES,
    GET_PLAYER_COUNT,
    CLEAN_PLAYER,
    REMOVE_PROXY,
    SET_PLAYER_SERVER;

    private static final String PATH = "lua/";
    private final @NonNull String script;
    private final @NonNull String hash;

    LuaScripts() {
        final ToastrPlugin instance = ToastrPlugin.getInstance();

        instance.getLogger().debug("[Lua] Loading script " + this.toString() + "(" + PATH + this.toString().toLowerCase().replace("_", "-") + ".lua)");

        this.script = ResourceReader.readResource(PATH + this.toString().toLowerCase().replace("_", "-") + ".lua");
        this.hash = instance.getRedisManager().createScript(script);
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
