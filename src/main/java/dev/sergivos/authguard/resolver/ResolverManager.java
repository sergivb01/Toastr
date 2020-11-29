package dev.sergivos.authguard.resolver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.sergivos.authguard.AuthGuard;
import dev.sergivos.authguard.resolver.impl.AshconResolver;
import dev.sergivos.authguard.resolver.impl.MineToolsResolver;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ResolverManager {
    protected static final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("AuthGuard Authenticator - %1$d")
            .build());
    private final Cache<String, Resolver.Result> responseCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .recordStats()
            .maximumSize(150)
            .removalListener((removed) -> AuthGuard.getInstance().getLogger().info("[CACHE] Removing " + removed.getKey() + " from cache (" + removed.getCause() + ")"))
            .build();
    private final Resolver[] resolvers = new Resolver[]{
            new AshconResolver(),
            new MineToolsResolver()
    };

    public Resolver.Result resolveUsername(String username) throws Exception {
        long start = System.currentTimeMillis();

        Resolver.Result cachedValue = responseCache.getIfPresent(username);
        if(cachedValue != null) {
            AuthGuard.getInstance().getLogger().info("[CACHE] [" + cachedValue.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (cachedValue.isPremium() ? "premium" : "cracked"));
            return cachedValue;
        }

        List<ResolverTask> resolverTasks = new ArrayList<>();
        for(Resolver resolver : resolvers) {
            resolverTasks.add(new ResolverTask(resolver, username));
        }

        Resolver.Result result = executor.invokeAny(resolverTasks, 1500, TimeUnit.MILLISECONDS);
        responseCache.put(username, result);

        AuthGuard.getInstance().getLogger().info("[" + result.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (result.isPremium() ? "premium" : "cracked"));

        return result;
    }

    public CacheStats getStats() {
        return responseCache.stats();
    }

    @AllArgsConstructor
    private static class ResolverTask implements Callable<Resolver.Result> {
        private final Resolver resolver;
        private final String username;

        @Override
        public Resolver.Result call() throws Exception {
            return resolver.check(username);
        }
    }

}
