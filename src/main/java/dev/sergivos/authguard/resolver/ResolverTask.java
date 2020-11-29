package dev.sergivos.authguard.resolver;

import lombok.AllArgsConstructor;

import java.util.concurrent.Callable;

@AllArgsConstructor
public class ResolverTask implements Callable<Resolver.Result> {
    private final Resolver resolver;
    private final String username;

    @Override
    public Resolver.Result call() throws Exception {
        return resolver.check(username);
    }
}
