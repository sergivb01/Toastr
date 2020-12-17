package services.vortex.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.util.UuidUtils;
import okhttp3.Request;
import okhttp3.Response;
import services.vortex.toastr.resolver.Resolver;

import java.util.UUID;

public class MineToolsResolver extends Resolver {
    private static final String MINETOOLS_URL = "https://api.minetools.eu/uuid/";

    @Override
    public String getSource() {
        return "MineTools";
    }

    @Override
    public Resolver.Result check(String rawUsername) throws Exception {
        Request request = new Request.Builder()
                .url(MINETOOLS_URL + rawUsername)
                .build();

        try(final Response response = httpClient.newCall(request).execute()) {
            if(response.code() != 200) {
                throw new Exception("Invalid status code from MineTools " + response.code());
            }

            final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();
            if(data.get("id").isJsonNull()) {
                return fromOffline(rawUsername);
            }

            String username = data.get("name").getAsString();
            UUID playerUUID = UuidUtils.fromUndashed(data.get("id").getAsString());

            return new Result(username, playerUUID, true, getSource());
        }
    }

}
