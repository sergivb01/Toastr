package dev.sergivos.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sergivos.toastr.resolver.Resolver;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import okhttp3.Response;

import java.util.UUID;

@RequiredArgsConstructor
public class AshconResolver extends Resolver {
    private static final String ASHCON_URL = "https://api.ashcon.app/mojang/v2/user/";
    private final String rawUsername;

    public String getSource() {
        return "Ashcon";
    }

    public Resolver.Result call() throws Exception {
        Request request = new Request.Builder()
                .url(ASHCON_URL + rawUsername)
                .build();

        try(final Response response = httpClient.newCall(request).execute()) {
            final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();

            if(response.code() == 404) {
                if(data.get("code").getAsInt() != 404) {
                    throw new Exception("Invalid status code from Ashcon " + response.code() + " but response-body doesn't respond 404");
                }

                return fromOffline(rawUsername);
            }

            if(response.code() != 200) {
                throw new Exception("Invalid status code from Ashcon " + response.code());
            }


            String username = data.get("username").getAsString();
            String rawUUID = data.get("uuid").getAsString();
            UUID playerUUID = UUID.fromString(rawUUID);

            return new Result(username, playerUUID, true, getSource());
        }
    }

}
