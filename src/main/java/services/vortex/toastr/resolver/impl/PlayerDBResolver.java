package services.vortex.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Request;
import okhttp3.Response;
import services.vortex.toastr.resolver.Resolver;

import java.util.UUID;

public class PlayerDBResolver extends Resolver {
    private static final String PLAYERDB_URL = "https://playerdb.co/api/player/minecraft/";

    public String getSource() {
        return "PlayerDB";
    }

    public Resolver.Result check(String rawUsername) throws Exception {
        Request request = new Request.Builder()
                .url(PLAYERDB_URL + rawUsername)
                .build();
        final Response response = httpClient.newCall(request).execute();

        if(response.code() != 200 && response.code() != 500) {
            response.body().close();
            throw new Exception("Invalid status code from PlayerDB " + response.code());
        }

        final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();
        response.body().close();

        if(!data.get("code").getAsString().equals("player.found")) {
            throw new Exception("Player not found but API can't verify player exists");
        }

        final JsonObject meta = data.get("data").getAsJsonObject().get("player").getAsJsonObject();

        final String username = meta.get("username").getAsString();
        final String rawUUID = meta.get("id").getAsString();
        UUID playerUUID = UUID.fromString(rawUUID);

        return new Result(username, playerUUID, true, getSource());
    }

}
