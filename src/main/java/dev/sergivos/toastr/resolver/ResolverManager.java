package dev.sergivos.toastr.resolver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.resolver.impl.AshconResolver;

import java.util.Collections;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ResolverManager {
    protected static final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("Toastr Authenticator - %1$d")
            .setDaemon(true)
            .build());
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    public Resolver.Result resolveUsername(String username) throws Exception {
        long start = System.currentTimeMillis();

        Resolver.Result result = instance.getCacheManager().getPlayerResult(username);
        if(result != null) {
            instance.getLogger().info("[resolver] [cache - " + result.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (result.isPremium() ? "premium" : "cracked"));
            return result;
        }

        // TODO: add other Resolvers
        result = executor.invokeAny(Collections.singletonList(
                new AshconResolver(username)
//                new CloudProtectedResolver(username),
//                new MineToolsResolver(username),
//                new PlayerDBResolver(username)
        ), 1500, TimeUnit.MILLISECONDS);

        instance.getRedisManager().setPlayerResult(username, result);

        instance.getLogger().info("[resolver] [" + result.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (result.isPremium() ? "premium" : "cracked"));

        return result;
    }

}
