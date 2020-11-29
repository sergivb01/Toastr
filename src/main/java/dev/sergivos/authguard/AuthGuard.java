package dev.sergivos.authguard;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.sergivos.authguard.commands.StatsCommand;
import dev.sergivos.authguard.listeners.PlayerPreLogin;
import dev.sergivos.authguard.resolver.ResolverManager;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "authguard",
        name = "Authguard",
        version = "1.0-SNAPSHOT",
        description = "A Velocity Powered authentication plugin for Premium/Cracked accounts",
        url = "https://sergivos.dev",
        authors = {"Sergi Vos"}
)
@Getter
public class AuthGuard {

    @Getter
    private static AuthGuard instance;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirector;
    private ResolverManager resolverManager;

    @Inject
    public AuthGuard(Logger logger, ProxyServer proxy, @DataDirectory Path dataDirector) {
        this.logger = logger;
        this.proxy = proxy;
        this.dataDirector = dataDirector;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        resolverManager = new ResolverManager();

        CommandManager commandManager = proxy.getCommandManager();
        commandManager.register("authstats", new StatsCommand());

        proxy.getEventManager().register(this, new PlayerPreLogin());
    }

}
