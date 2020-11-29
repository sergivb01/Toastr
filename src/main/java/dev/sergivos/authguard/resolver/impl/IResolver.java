package dev.sergivos.authguard.resolver.impl;

import dev.sergivos.authguard.resolver.Resolver;

public interface IResolver {

    String getSource();

    Resolver.Result check(String username) throws Exception;

}
