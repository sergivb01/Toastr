package dev.sergivos.authguard.resolver;

public interface IResolver {

    String getSource();

    Resolver.Result check(String username) throws Exception;

}
