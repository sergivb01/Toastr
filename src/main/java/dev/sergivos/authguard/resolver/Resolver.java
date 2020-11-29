package dev.sergivos.authguard.resolver;

import dev.sergivos.authguard.AuthGuard;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.asynchttpclient.AsyncHttpClient;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

public abstract class Resolver implements IResolver {
    protected AsyncHttpClient httpClient = asyncHttpClient(config()
            .setMaxConnections(500)
            .setMaxConnectionsPerHost(100)
            .setPooledConnectionIdleTimeout(100)
            .setConnectionTtl(500)
            .setReadTimeout(750)
            .setConnectTimeout(1000)
            .setRequestTimeout(1000)
            .setUserAgent("AuthGuard / 1.0")
    );
    protected static AuthGuard instance = AuthGuard.getInstance();

    @AllArgsConstructor
    @Getter
    public static
    class Result {
        private final boolean isPremium;
        private final String source;
    }

}
