package dev.sergivos.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sergivos.toastr.resolver.Resolver;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import okhttp3.Response;

import java.util.UUID;

@RequiredArgsConstructor
public class PlayerDBResolver extends Resolver {
    private static final String PLAYERDB_URL = "https://playerdb.co/api/player/minecraft/";
    private final String rawUsername;

    @Override
    public String getSource() {
        return "PlayerDB";
    }

    @Override
    public Resolver.Result call() throws Exception {
        Request request = new Request.Builder()
                .url(PLAYERDB_URL + rawUsername)
                .build();

        try(final Response response = httpClient.newCall(request).execute()) {
            if(response.code() != 200) {
                throw new Exception("Invalid status code from PlayerDB " + response.code());
            }

            final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();
            if(data.get("code").getAsString().equals("minecraft.api_failure")) {
                return fromOffline(rawUsername);
            }

            if(!data.get("code").getAsString().equals("player.found")) {
                throw new Exception("Player not found but " + getSource() + " can't verify player exists");
            }

            final JsonObject meta = data.get("data").getAsJsonObject().get("player").getAsJsonObject();

            final String username = meta.get("username").getAsString();
            final String rawUUID = meta.get("id").getAsString();
            UUID playerUUID = UUID.fromString(rawUUID);

            return new Result(username, playerUUID, true, getSource());
        }
    }

}
