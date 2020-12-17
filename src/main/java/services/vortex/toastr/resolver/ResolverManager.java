package services.vortex.toastr.resolver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AllArgsConstructor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.resolver.impl.AshconResolver;
import services.vortex.toastr.resolver.impl.MineToolsResolver;
import services.vortex.toastr.resolver.impl.PlayerDBResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ResolverManager {
    protected static final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 30L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("Toastr Authenticator - %1$d")
            .setDaemon(true)
            .build());
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private final Resolver[] resolvers = new Resolver[]{
            new AshconResolver(),
            new MineToolsResolver(),
            new PlayerDBResolver()
    };

    public Resolver.Result resolveUsername(String username) throws Exception {
        long start = System.currentTimeMillis();

        Resolver.Result result = instance.getCacheManager().getPlayerResult(username);
        if(result != null) {
            instance.getLogger().info("[resolver] [cache - " + result.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (result.isPremium() ? "premium" : "cracked"));
            return result;
        }

        List<ResolverTask> resolverTasks = new ArrayList<>();
        for(Resolver resolver : resolvers) {
            resolverTasks.add(new ResolverTask(resolver, username));
        }

        result = executor.invokeAny(resolverTasks, 1500, TimeUnit.MILLISECONDS);
        instance.getRedisManager().setPlayerResult(username, result);

        instance.getLogger().info("[resolver] [" + result.getSource() + "] Lookup for " + username + " took " + (System.currentTimeMillis() - start) + " ms. User is " + (result.isPremium() ? "premium" : "cracked"));

        return result;
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
