package services.vortex.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Request;
import okhttp3.Response;
import services.vortex.toastr.resolver.Resolver;

import java.util.UUID;

public class AshconResolver extends Resolver {
    private static final String ASHCON_URL = "https://api.ashcon.app/mojang/v2/user/";

    public String getSource() {
        return "Ashcon";
    }

    public Resolver.Result check(String rawUsername) throws Exception {
        Request request = new Request.Builder()
                .url(ASHCON_URL + rawUsername)
                .build();
        final Response response = httpClient.newCall(request).execute();

        if(response.code() == 404) {
            response.body().close();
            return fromOffline(rawUsername);
        }

        if(response.code() != 200) {
            response.body().close();
            throw new Exception("Invalid status code from Ashcon " + response.code());
        }

        final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();
        response.body().close();

        String username = data.get("username").getAsString();
        String rawUUID = data.get("uuid").getAsString();
        UUID playerUUID = UUID.fromString(rawUUID);
        boolean isSpoofed = !username.equals(rawUsername);

        return new Result(username, playerUUID, true, isSpoofed, getSource());
    }

}
