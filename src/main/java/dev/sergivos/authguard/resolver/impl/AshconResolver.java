package dev.sergivos.authguard.resolver.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sergivos.authguard.resolver.Resolver;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

public class AshconResolver extends Resolver {
    private static final String ASHCON_URL = "https://api.ashcon.app/mojang/v2/user/";

    @Override
    public String getSource() {
        return "Ashcon";
    }

    @Override
    public Resolver.Result check(String username) throws Exception {
        ListenableFuture<Response> res = httpClient.prepareGet(ASHCON_URL + username).execute();
        Response response = res.get();

        if(response.getStatusCode() != 200) {
            throw new Exception("Invalid status code from Ashcon " + response.getStatusCode());
        }

        JsonObject data = JsonParser.parseString(response.getResponseBody()).getAsJsonObject();

        return new Result(data.get("username").getAsString().equals(username), getSource());
    }

}
