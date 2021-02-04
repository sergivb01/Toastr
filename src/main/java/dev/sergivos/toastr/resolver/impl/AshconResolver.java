package dev.sergivos.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sergivos.toastr.resolver.Resolver;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class AshconResolver extends Resolver {
    private static final String ASHCON_URL = "https://api.ashcon.app/mojang/v2/uuid/";
    private final String rawUsername;

    public String getSource() {
        return "Ashcon";
    }

    public Resolver.Result call() throws Exception {
        Request request = new Request.Builder()
                .url(ASHCON_URL + rawUsername)
                .build();

        try(final Response response = httpClient.newCall(request).execute()) {
            if(response.body() == null) {
                throw new Exception("No content for " + getSource());
            }

            final String contentType = response.header("content-type");
            if(contentType == null) {
                throw new Exception("No content-type for " + getSource());
            }

            if(contentType.equals("text/plain") && response.code() == 200) {
                final String rawUUID = Objects.requireNonNull(response.body()).string();

                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUUID);
                } catch(Exception ex) {
                    throw new Exception("Failed to parse UUID " + rawUUID, ex);
                }

                return new Result(rawUsername, uuid, true, getSource());
            }

            if(contentType.equals("application/json") && response.code() == 404) {
                final JsonObject data = JsonParser.parseReader(response.body().charStream()).getAsJsonObject();

                final int code = data.get("code").getAsInt();
                if(code != 404) {
                    throw new Exception("unexpected json code in " + getSource() + ": " + code);
                }

                final String error = data.get("error").getAsString();
                if(!error.equals("Not Found")) {
                    throw new Exception("unexpected json error in " + getSource() + ": " + error);
                }

                final String reason = data.get("reason").getAsString();
                if(!reason.equals("No user with the name '" + rawUsername + "' was found")) {
                    throw new Exception("unexpected json reason in " + getSource() + ": " + reason);
                }

                return fromOffline(rawUsername);
            }

            throw new Exception("Unhandled combination of content-type and response-code: " + contentType + ", " + response.code());
        }
    }

}
