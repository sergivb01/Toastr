package dev.sergivos.toastr;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.sergivos.toastr.backend.BackendCredentials;
import dev.sergivos.toastr.backend.mysql.BackendStorage;
import dev.sergivos.toastr.backend.redis.CacheManager;
import dev.sergivos.toastr.backend.redis.RedisManager;
import dev.sergivos.toastr.commands.BetaCommand;
import dev.sergivos.toastr.commands.admin.*;
import dev.sergivos.toastr.commands.auth.*;
import dev.sergivos.toastr.commands.essentials.GListCommand;
import dev.sergivos.toastr.commands.essentials.GlobalMessageCommand;
import dev.sergivos.toastr.commands.essentials.LobbyCommand;
import dev.sergivos.toastr.commands.essentials.ProfileCommand;
import dev.sergivos.toastr.listeners.*;
import dev.sergivos.toastr.lobbby.LobbyManager;
import dev.sergivos.toastr.resolver.ResolverManager;
import dev.sergivos.toastr.utils.CC;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Plugin(
        id = "toastr",
        name = "Toastr",
        version = "1.0-SNAPSHOT",
        description = "A Velocity Powered authentication plugin for Premium/Cracked accounts",
        url = "https://sergivos.dev",
        authors = {"Sergi Vos"}
)
@Getter
public class ToastrPlugin {

    @Getter
    private static ToastrPlugin instance;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirector;
    private Configuration config;

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

        if(!loadConfig()) return;

        resolverManager = new ResolverManager();
        lobbyManager = new LobbyManager();
        lobbyManager.loadLobbies();

        final Configuration redisConfig = config.getSection("database.redis");
        redisManager = new RedisManager(new BackendCredentials(redisConfig.getString("host"), redisConfig.getInt("port"),
                null, redisConfig.getString("password"), null));
        cacheManager = new CacheManager();

        final Configuration mysqlConfig = config.getSection("database.mysql");
        backendStorage = new BackendStorage(new BackendCredentials(mysqlConfig.getString("host"), mysqlConfig.getInt("port"),
                mysqlConfig.getString("username"), mysqlConfig.getString("password"), mysqlConfig.getString("database")));

        CommandManager commandManager = proxy.getCommandManager();

        commandManager.register("alert", new AlertCommand());
        commandManager.register("clearcache", new ClearCacheCommand());
        commandManager.register("toastrl", new ReloadCommand());
        commandManager.register("send", new SendCommand());
        commandManager.register("sendtoall", new SendToAllCommand());
        commandManager.register("serverid", new ServerIDCommand());

        commandManager.register("changepassword", new ChangePasswordCommand());
        commandManager.register("login", new LoginCommand(), "l");
        commandManager.register("register", new RegisterCommand(), "reg");
        commandManager.register("unregister", new UnRegisterCommand());
        commandManager.register("aunregister", new UnRegisterOtherCommand());

        commandManager.unregister("glist");
        commandManager.register("glist", new GListCommand());
        commandManager.register("gmsg", new GlobalMessageCommand());
        commandManager.register("lobby", new LobbyCommand(), "hub");
        commandManager.register("tprofile", new ProfileCommand());
        commandManager.register("beta", new BetaCommand());

        Arrays.asList(
                new AuthListener(),
                new LobbyListener(),
                new NetworkListener(),
                new PlayerListener(),
                new PluginMessageListener(),
                new RateLimitListener()
        ).forEach(listener -> proxy.getEventManager().register(this, listener));
    }

    @SneakyThrows
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        new Thread(() -> resolverManager.shutdown(), "Resolver shutdown").start();
        new Thread(() -> redisManager.shutdown(), "Redis shutdown").start();

        backendStorage.shutdown();
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        if(!loadConfig() || config == null) {
            logger.error("Can not reload config");
            return;
        }
        lobbyManager.loadLobbies();

        logger.info("reloaded config after proxy-reload");
    }

    /**
     * This method registers the Config files
     *
     * @return True if loaded successfully
     */
    public boolean loadConfig() {
        File config = new File(dataDirector.toFile(), "config.yml");
        config.getParentFile().mkdir();
        try {
            if(!config.exists()) {
                try(InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    Files.copy(in, config.toPath());
                }
            }
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(config);
        } catch(Exception ex) {
            logger.error("Can not load or save config", ex);
            return false;
        }
        return true;
    }

    /**
     * This method loads a Component using MiniMessage
     *
     * @param name         The message name
     * @param placeholders The placeholders
     * @return The Component
     */
    public Component getMessage(String name, String... placeholders) {
        return CC.translate(config.getString("messages." + name), placeholders);
    }

    public Collection<Component> getMessages(String name, String... placeholders) {
        Collection<Component> res = new ArrayList<>();

        final List<String> messages = config.getStringList("messages." + name);
        for(String msg : messages) {
            res.add(CC.translate(msg, placeholders));
        }

        return res;
    }

}
