package dev.sergivos.authguard.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.util.UuidUtils;
import dev.sergivos.authguard.resolver.Resolver;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

import java.util.UUID;

public class MineToolsResolver extends Resolver {
    private static String MINETOOLS_URL = "https://api.minetools.eu/uuid/";

    @Override
    public String getSource() {
        return "MineTools";
    }

    @Override
    public Resolver.Result check(String rawUsername) throws Exception {
        ListenableFuture<Response> res = httpClient.prepareGet(MINETOOLS_URL + rawUsername).execute();
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
        String rawUUID = data.get("id").getAsString();
        UUID playerUUID = UuidUtils.fromUndashed(rawUUID);
        boolean isSpoofed = !username.equals(rawUsername);

        Result result = new Result(username, playerUUID, true, isSpoofed, getSource());
        instance.getLogger().info("[" + getSource() + "] " + result.toString());

        return result;
    }

}
