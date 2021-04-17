package dev.sergivos.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sergivos.toastr.resolver.Resolver;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import okhttp3.Response;

import java.util.UUID;

@RequiredArgsConstructor
public class CloudProtectedResolver extends Resolver {
    private static final String CLOUDPROTECTED_URL = "https://mcapi.cloudprotected.net/uuid/";
    private final String rawUsername;

    @Override
    public String getSource() {
        return "CloudProtected";
    }

    public Resolver.Result call() throws Exception {
        Request request = new Request.Builder()
                .url(CLOUDPROTECTED_URL + rawUsername)
                .build();

        try(final Response response = httpClient.newCall(request).execute()) {
            if(response.code() != 200) {
                throw new Exception("Invalid status code from CloudProtected " + response.code());
            }

            final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject().get("result").getAsJsonObject().get("0").getAsJsonObject();

            String username = data.get("name").getAsString();
            String rawUUID = data.get("uuid-formatted").getAsString();
            UUID playerUUID = UUID.fromString(rawUUID);

            return new Resolver.Result(username, playerUUID, true, getSource());
        }
    }

}
