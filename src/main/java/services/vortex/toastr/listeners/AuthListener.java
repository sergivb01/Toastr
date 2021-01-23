package services.vortex.toastr.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.resolver.Resolver;
import services.vortex.toastr.tasks.LoginTask;
import services.vortex.toastr.tasks.RegisterTask;
import services.vortex.toastr.utils.CC;
import services.vortex.toastr.utils.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.*;

public class AuthListener {
    public static final ConcurrentHashMap<Player, Long> pendingRegister = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Player, Long> pendingLogin = new ConcurrentHashMap<>();
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    public AuthListener() {
        instance.getProxy().getScheduler().buildTask(instance, new RegisterTask(pendingRegister)).repeat(5, TimeUnit.SECONDS).schedule();
        instance.getProxy().getScheduler().buildTask(instance, new LoginTask(pendingLogin)).repeat(5, TimeUnit.SECONDS).schedule();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerPreLogin(PreLoginEvent event) {
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
            result = instance.getBackendStorage().checkAccounts(player).get(3, TimeUnit.SECONDS);
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
            profile = instance.getBackendStorage().getProfile(player.getUniqueId()).get(3, TimeUnit.SECONDS);
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

        if(profile.isLoggedIn()) return;

        if(StringUtils.isNullOrEmpty(profile.getPassword())) {
            pendingRegister.put(player, System.currentTimeMillis());
        } else {
            pendingLogin.put(player, System.currentTimeMillis());
        }

        boolean newPlayer = false;
        try {
            newPlayer = instance.getBackendStorage().saveProfile(profile).get(3, TimeUnit.SECONDS);
        } catch(Exception ex) {
            instance.getLogger().error("Error saving profile for " + player.getUsername() + " after login", ex);
            player.disconnect(Component.text("Failed to save your profile after login.\nContact an administrator").color(NamedTextColor.RED));
            return;
        }

        // TODO: first time logging in. Implement something maybe? (:
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
            instance.getBackendStorage().saveProfile(profile).get(3, TimeUnit.SECONDS);
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

        final JsonArray allowedCommands = instance.getConfig().getObject().getAsJsonObject("auth").getAsJsonArray("allowed_commands");
        for(JsonElement allowed : allowedCommands) {
            if(event.getCommand().toLowerCase().contains(allowed.getAsString())) {
                return;
            }
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
