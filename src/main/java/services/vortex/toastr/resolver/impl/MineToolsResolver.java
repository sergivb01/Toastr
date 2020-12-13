package services.vortex.toastr.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.util.UuidUtils;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
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
        ListenableFuture<Response> res = httpClient.prepareGet(MINETOOLS_URL + rawUsername).execute();
        Response response = res.get();

        if(response.getStatusCode() != 200) {
            throw new Exception("Invalid status code from MineTools " + response.getStatusCode());
        }

        JsonObject data = JsonParser.parseString(response.getResponseBody()).getAsJsonObject();

        if(data.get("id").isJsonNull()) {
            // TODO: should check lowercase stuff to prevent stealing accounts with same nickname but different lowerCase/upperCase
            return fromOffline(rawUsername);
        }

        String username = data.get("name").getAsString();
        String rawUUID = data.get("id").getAsString();
        UUID playerUUID = UuidUtils.fromUndashed(rawUUID);
        boolean isSpoofed = !username.equals(rawUsername);

        return new Result(username, playerUUID, true, isSpoofed, getSource());
    }

}
