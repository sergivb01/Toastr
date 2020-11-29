package services.vortex.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import services.vortex.toastr.resolver.Resolver;

import java.util.UUID;

public class AshconResolver extends Resolver {
    private static final String ASHCON_URL = "https://api.ashcon.app/mojang/v2/user/";

    @Override
    public String getSource() {
        return "Ashcon";
    }

    @Override
    public Resolver.Result check(String rawUsername) throws Exception {
        ListenableFuture<Response> res = httpClient.prepareGet(ASHCON_URL + rawUsername).execute();
        Response response = res.get();

        if(response.getStatusCode() == 404) {
            // TODO: should check lowercase stuff to prevent stealing accounts with same nickname but different lowerCase/upperCase
            return fromOffline(rawUsername);
        }

        if(response.getStatusCode() != 200) {
            throw new Exception("Invalid status code from Ashcon " + response.getStatusCode());
        }

        JsonObject data = JsonParser.parseString(response.getResponseBody()).getAsJsonObject();

        String username = data.get("username").getAsString();
        String rawUUID = data.get("uuid").getAsString();
        UUID playerUUID = UUID.fromString(rawUUID);
        boolean isSpoofed = !username.equals(rawUsername);

        Result result = new Result(username, playerUUID, true, isSpoofed, getSource());
        instance.getLogger().info("[" + getSource() + "] " + result.toString());

        return result;
    }

}
