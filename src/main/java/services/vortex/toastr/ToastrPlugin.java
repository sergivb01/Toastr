package services.vortex.toastr;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import services.vortex.toastr.backend.mysql.BackendCredentials;
import services.vortex.toastr.backend.mysql.BackendStorage;
import services.vortex.toastr.backend.redis.CacheManager;
import services.vortex.toastr.backend.redis.RedisManager;
import services.vortex.toastr.commands.admin.*;
import services.vortex.toastr.commands.auth.ChangePasswordCommand;
import services.vortex.toastr.commands.auth.LoginCommand;
import services.vortex.toastr.commands.auth.RegisterCommand;
import services.vortex.toastr.commands.auth.UnRegisterCommand;
import services.vortex.toastr.commands.essentials.LobbyCommand;
import services.vortex.toastr.listeners.*;
import services.vortex.toastr.lobbby.LobbyManager;
import services.vortex.toastr.resolver.ResolverManager;
import services.vortex.toastr.utils.Config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Plugin(
        id = "toastr",
        name = "Toastr",
        version = "1.0-SNAPSHOT",
        description = "A Velocity Powered authentication plugin for Premium/Cracked accounts",
        url = "https://sergivos.dev",
        authors = {"Sergi Vos", "Vortex Serivces"},
        dependencies = {@Dependency(id = "tcpshield")}
)
@Getter
public class ToastrPlugin {

    @Getter
    private static ToastrPlugin instance;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirector;
    private Config config;

    private ResolverManager resolverManager;
    private BackendStorage backendStorage;
    private LobbyManager lobbyManager;

    private RedisManager redisManager;
    private CacheManager cacheManager;

    @Inject
    public ToastrPlugin(Logger logger, ProxyServer proxy, @DataDirectory Path dataDirector) {
        this.logger = logger;
        this.proxy = proxy;
        this.dataDirector = dataDirector;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        if(!registerConfigs()) return;

        resolverManager = new ResolverManager();
        lobbyManager = new LobbyManager();
        lobbyManager.loadLobbies();

        redisManager = new RedisManager();
        cacheManager = new CacheManager();

        final JsonObject dbConfig = config.getObject().getAsJsonObject("database");

        backendStorage = new BackendStorage(new BackendCredentials(dbConfig.get("host").getAsString(), dbConfig.get("port").getAsInt(), dbConfig.get("username").getAsString(), dbConfig.get("password").getAsString(), dbConfig.get("database").getAsString()));

        CommandManager commandManager = proxy.getCommandManager();

        commandManager.register("alert", new AlertCommand());
        commandManager.unregister("glist");
        commandManager.register("glist", new GListCommand());
        commandManager.register("tprofile", new ProfileCommand());
        commandManager.register("toastrl", new ReloadCommand());
        commandManager.register("sendtoall", new SendToAllCommand());
        commandManager.register("serverid", new ServerIDCommand());

        commandManager.register("changepassword", new ChangePasswordCommand());
        commandManager.register("login", new LoginCommand());
        commandManager.register("register", new RegisterCommand());
        commandManager.register("unregister", new UnRegisterCommand());

        commandManager.register("lobby", new LobbyCommand());

        Arrays.asList(
                new AuthListener(),
                new LobbyListener(),
                new NetworkListener(),
                new PlayerListener(),
                new PluginMessageListener()
        ).forEach(listener -> proxy.getEventManager().register(this, listener));
    }

    @SneakyThrows
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        final Thread shutdown = new Thread(() -> redisManager.shutdown(), "Redis shutdown");
        shutdown.start();

        backendStorage.shutdown();
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        try {
            config.reload();
            lobbyManager.loadLobbies();

            logger.info("reloaded config after proxy-reload");
        } catch(FileNotFoundException e) {
            instance.getLogger().error("Error trying to reload config after proxy-reload!", e);
        }
    }

    /**
     * This method registers the Config files
     *
     * @return True if loaded successfully
     */
    private boolean registerConfigs() {
        try {
            config = new Config(this, dataDirector, "config");
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            logger.error("An error occurred while loading the config file");
            return false;
        }
    }

}
