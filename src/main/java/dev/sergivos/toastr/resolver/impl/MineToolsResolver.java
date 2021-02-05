package dev.sergivos.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.util.UuidUtils;
import dev.sergivos.toastr.resolver.Resolver;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import okhttp3.Response;

import java.util.UUID;

@RequiredArgsConstructor
public class MineToolsResolver extends Resolver {
    private static final String MINETOOLS_URL = "https://api.minetools.eu/uuid/";
    private final String rawUsername;

    @Override
    public String getSource() {
        return "MineTools";
    }

    @Override
    public Resolver.Result call() throws Exception {
        Request request = new Request.Builder()
                .url(MINETOOLS_URL + rawUsername)
                .build();

        try(final Response response = httpClient.newCall(request).execute()) {
            if(response.code() != 200) {
                throw new Exception("Invalid status code from MineTools " + response.code());
            }

            final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();
            if(data.get("id").isJsonNull() && data.get("status").getAsString().equals("ERR")) {
                return fromOffline(rawUsername);
            }

            String username = data.get("name").getAsString();
            UUID playerUUID = UuidUtils.fromUndashed(data.get("id").getAsString());

            return new Result(username, playerUUID, true, getSource());
        }
    }

}
