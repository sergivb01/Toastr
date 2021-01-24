package dev.sergivos.toastr.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.profile.Profile;
import dev.sergivos.toastr.resolver.Resolver;
import dev.sergivos.toastr.tasks.LoginTask;
import dev.sergivos.toastr.tasks.RegisterTask;
import dev.sergivos.toastr.utils.CC;
import dev.sergivos.toastr.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.*;

public class AuthListener {
    public static final ConcurrentHashMap<Player, Long> pendingRegister = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Player, Long> pendingLogin = new ConcurrentHashMap<>();
    private static final Pattern validUsername = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    private static final int MIN_VERSION = ProtocolVersion.MINECRAFT_1_7_2.getProtocol();
    private static final int MAX_VERSION = ProtocolVersion.MINECRAFT_1_8.getProtocol();
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    public AuthListener() {
        instance.getProxy().getScheduler().buildTask(instance, new RegisterTask(pendingRegister)).repeat(5, TimeUnit.SECONDS).schedule();
        instance.getProxy().getScheduler().buildTask(instance, new LoginTask(pendingLogin)).repeat(5, TimeUnit.SECONDS).schedule();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerPreLogin(PreLoginEvent event) {
        final ProtocolVersion version = event.getConnection().getProtocolVersion();
        if(version.isUnknown() || version.getProtocol() < MIN_VERSION || version.getProtocol() > MAX_VERSION) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CC.translate("&CUnsupported version\nPlease use clients 1.7.X or 1.8.X")));
            return;
        }

        if(!validUsername.matcher(event.getUsername()).matches()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Invalid username\nPlease contact administrator").color(NamedTextColor.RED)));
            return;
        }

        try {
            Resolver.Result result = instance.getResolverManager().resolveUsername(event.getUsername());
            if(!result.getUsername().equals(event.getUsername())) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Spoof #1 detected")));
                return;
            }

            if(result.isPremium()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            } else {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            }
        } catch(Exception ex) {
            instance.getLogger().error("Error getting result for " + event.getUsername(), ex);
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("error in backend")));
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        final Optional<Player> player = instance.getProxy().getPlayer(event.getOriginalProfile().getId());

        final Resolver.Result result = instance.getCacheManager().getPlayerResult(event.getUsername());
        if(result == null) {
            player.ifPresent(target -> target.disconnect(Component.text("Internal Toastr error #1").color(NamedTextColor.RED)));
            instance.getLogger().warn("Tried to check for spoof #2 and #3 for " + event.getUsername() + ", ResolverResult is null");
            return;
        }

        final GameProfile gameProfile = event.getGameProfile();
        if(!gameProfile.getName().equals(result.getUsername())) {
            player.ifPresent(target -> target.disconnect(Component.text("Spoof #2").color(NamedTextColor.RED)));
            return;
        }

        if(!gameProfile.getId().equals(result.getUniqueId())) {
            player.ifPresent(target -> target.disconnect(Component.text("Spoof #3").color(NamedTextColor.RED)));
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();

        // TODO: check this. Who deserves the right of using the account?
        final Profile.CheckAccountResult result;
        try {
            result = instance.getBackendStorage().checkAccounts(player);
            if(result.equals(Profile.CheckAccountResult.DIFFERENT_NAMECASE)) {
                player.disconnect(Component.text("Different namecase! Contact admin").color(NamedTextColor.RED));
                instance.getLogger().warn("Player with UUID " + player.getUniqueId() + " and username " + player.getUsername() + " tried to login with different namecase " + player.getRemoteAddress().toString());
                return;
            }

            if(result.equals(Profile.CheckAccountResult.OLD_PREMIUM)) {
                player.disconnect(Component.text("This username was premium in the past < 37 days\nPlease login with another account or contact an administrator").color(NamedTextColor.RED));
                instance.getLogger().warn("Player with UUID " + player.getUniqueId() + " and username " + player.getUsername() + " tried to login with an old premium account " + player.getRemoteAddress().toString());
                return;
            }
        } catch(Exception ex) {
            instance.getLogger().error("Error checking namecase for " + player.getUsername(), ex);
            player.disconnect(Component.text("Error trying to check namecase!").color(NamedTextColor.RED));
            return;
        }


        if(!result.equals(Profile.CheckAccountResult.ALLOWED)) {
            return;
        }

        Profile profile;
        try {
            profile = instance.getBackendStorage().getProfile(player.getUniqueId());
        } catch(Exception ex) {
            instance.getLogger().error("Error loading profile for " + player.getUsername(), ex);
            player.disconnect(Component.text("Error loading your profile!").color(NamedTextColor.RED));
            return;
        }

        boolean autoLogin = false;
        if(profile == null) {
            profile = Profile.createProfile(player);
        } else {
            autoLogin = profile.getLastIP().equals(player.getRemoteAddress().getAddress().getHostAddress())
                    && (System.currentTimeMillis() - profile.getLastLogin().getTime()) < TimeUnit.MINUTES.toMillis(15);
        }

        profile.setLastLogin(Timestamp.from(Instant.now()));
        profile.setLastIP(player.getRemoteAddress().getAddress().getHostAddress());
        profile.setLoggedIn(player.isOnlineMode() || autoLogin);

        Profile.getProfiles().put(player.getUniqueId(), profile);
        player.sendMessage(Component.text("Your profile has been loaded!").color(NamedTextColor.DARK_AQUA));
        if(autoLogin) {
            player.showTitle(Title.title(CC.translate("&2Auto logged in"), CC.translate("Recovered last session")));
        }

        boolean newPlayer = false;
        try {
            newPlayer = instance.getBackendStorage().saveProfile(profile);
        } catch(Exception ex) {
            instance.getLogger().error("Error saving profile for " + player.getUsername() + " after login", ex);
            player.disconnect(Component.text("Failed to save your profile after login.\nContact an administrator").color(NamedTextColor.RED));
            return;
        }
        // TODO: first time logging in. Implement something maybe? (:

        if(profile.isLoggedIn()) return;

        if(StringUtils.isNullOrEmpty(profile.getPassword())) {
            pendingRegister.put(player, System.currentTimeMillis());
        } else {
            pendingLogin.put(player, System.currentTimeMillis());
        }
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        final Player player = event.getPlayer();

        pendingRegister.remove(player);
        pendingLogin.remove(player);

        final DisconnectEvent.LoginStatus loginStatus = event.getLoginStatus();
        if(loginStatus.equals(CONFLICTING_LOGIN) || loginStatus.equals(CANCELLED_BY_USER_BEFORE_COMPLETE) || loginStatus.equals(PRE_SERVER_JOIN)) {
            return;
        }

        final Profile profile = Profile.getProfiles().get(player.getUniqueId());
        if(profile == null) {
            return;
        }

        try {
            instance.getBackendStorage().saveProfile(profile);
        } catch(Exception ex) {
            instance.getLogger().error("Saving " + player.getUsername() + " profile!", ex);
            return;
        }

        Profile.getProfiles().remove(player.getUniqueId());
    }

    @Subscribe
    public void onPlayerCommand(CommandExecuteEvent event) {
        if(!(event.getCommandSource() instanceof Player))
            return;

        if(instance.getConfig().getStringList("auth.allowed_commands").contains(event.getCommand().toLowerCase())) {
            return;
        }

        Player player = (Player) event.getCommandSource();
        Profile profile = Profile.getProfiles().get(player.getUniqueId());

        if(profile != null && profile.isLoggedIn()) return;

        player.sendMessage(Component.text("You may not execute this command without being logging in!").color(NamedTextColor.RED));
        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        Profile profile = Profile.getProfiles().get(player.getUniqueId());

        if(profile != null && profile.isLoggedIn()) return;

        player.sendMessage(Component.text("You may not send chat messages without being logging in!").color(NamedTextColor.RED));
        event.setResult(PlayerChatEvent.ChatResult.denied());
    }

    @Subscribe
    public void onChange(ServerPreConnectEvent event) {
        if(event.getOriginalServer() == null) return;

        final Optional<RegisteredServer> server = event.getResult().getServer();
        if(server.isPresent() && instance.getLobbyManager().isLobby(server.get())) return;

        Profile profile = Profile.getProfiles().get(event.getPlayer().getUniqueId());
        if(profile == null || !profile.isLoggedIn()) {
            event.getPlayer().sendMessage(Component.text("You may not switch servers without logging in!").color(NamedTextColor.RED));
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

}
