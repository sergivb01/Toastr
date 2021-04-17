package dev.sergivos.toastr.resolver;

import com.velocitypowered.api.util.UuidUtils;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.resolver.impl.IResolver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import okhttp3.OkHttpClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class Resolver implements IResolver<Resolver.Result> {
    protected static final ToastrPlugin instance = ToastrPlugin.getInstance();
    protected static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build();

    protected Result fromOffline(String rawUsername) {
        return new Result(rawUsername, UuidUtils.generateOfflinePlayerUuid(rawUsername), false, getSource());
    }

    @AllArgsConstructor
    @Getter
    @ToString
    public static
    class Result {
        private final String username;
        private final UUID uniqueId;
        private final boolean premium;
        private final String source;
    }

}
