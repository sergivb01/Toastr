package dev.sergivos.authguard.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sergivos.authguard.resolver.Resolver;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

public class MineToolsResolver extends Resolver {
    private static String MINETOOLS_URL = "https://api.minetools.eu/uuid/";

    @Override
    public String getSource() {
        return "MineTools";
    }

    @Override
    public Resolver.Result check(String username) throws Exception {
        ListenableFuture<Response> res = httpClient.prepareGet(MINETOOLS_URL + username).execute();
        Response response = res.get();

        if(response.getStatusCode() != 200) {
            throw new Exception("Invalid status code from Ashcon " + response.getStatusCode());
        }

        JsonObject data = JsonParser.parseString(response.getResponseBody()).getAsJsonObject();

        return new Result(data.get("name").getAsString().equals(username) &&
                !data.get("status").getAsString().equals("ERR"), getSource());
    }

}
