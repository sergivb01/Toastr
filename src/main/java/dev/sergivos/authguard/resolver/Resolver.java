package dev.sergivos.authguard.resolver;

import com.velocitypowered.api.util.UuidUtils;
import dev.sergivos.authguard.AuthGuard;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.asynchttpclient.AsyncHttpClient;

import java.util.UUID;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

public abstract class Resolver implements IResolver {
    protected AsyncHttpClient httpClient = asyncHttpClient(config()
            .setMaxConnections(500)
            .setMaxConnectionsPerHost(100)
            .setPooledConnectionIdleTimeout(100)
            .setConnectionTtl(500)
            .setReadTimeout(750)
            .setConnectTimeout(1500)
            .setRequestTimeout(1500)
            .setUserAgent("AuthGuard / 1.0")
    );
    protected static AuthGuard instance = AuthGuard.getInstance();

    protected Result fromOffline(String rawUsername){
        return new Result(rawUsername, UuidUtils.generateOfflinePlayerUuid(rawUsername), false, false, getSource());
    }

    @AllArgsConstructor
    @Getter
    @ToString
    public static
    class Result {
        private final String username;
        private final UUID playerUUID;
        private final boolean isPremium;
        private final boolean isSpoofed;
        private final String source;
    }

}
