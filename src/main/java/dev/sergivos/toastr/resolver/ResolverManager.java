package dev.sergivos.toastr.resolver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.resolver.impl.AshconResolver;
import dev.sergivos.toastr.resolver.impl.CloudProtectedResolver;
import dev.sergivos.toastr.resolver.impl.MineToolsResolver;
import dev.sergivos.toastr.resolver.impl.PlayerDBResolver;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ResolverManager {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final ExecutorService executor = new ThreadPoolExecutor(8, Integer.MAX_VALUE, 15L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("Toastr Authenticator - %1$d")
            .setDaemon(true)
            .build());

    public void shutdown() {
        executor.shutdownNow();
    }

    public Resolver.Result resolveUsername(String username) throws Exception {
        long start = System.currentTimeMillis();

        Resolver.Result result = instance.getCacheManager().getPlayerResult(username).orElse(null);
        if(result != null) {
            instance.getLogger().debug("[resolver] [cache/" + result.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (result.isPremium() ? "premium" : "cracked"));
            return result;
        }

        result = executor.invokeAny(Arrays.asList(
                new AshconResolver(username),
                new CloudProtectedResolver(username),
                new MineToolsResolver(username),
                new PlayerDBResolver(username)
        ), 1500, TimeUnit.MILLISECONDS);

        instance.getRedisManager().setPlayerResult(username, result);

        instance.getLogger().debug("[resolver] [" + result.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (result.isPremium() ? "premium" : "cracked"));

        return result;
    }

}
