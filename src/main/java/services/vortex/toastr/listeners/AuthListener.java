package services.vortex.toastr.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.resolver.Resolver;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AuthListener {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Subscribe
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
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("error in backend")));
            ex.printStackTrace();
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

        instance.getBackendStorage().getProfile(player.getUniqueId())
                .whenComplete((profile, ex) -> {
                    if(ex != null) {
                        ex.printStackTrace();
                        player.disconnect(Component.text("Error loading your profile!").color(NamedTextColor.RED));
                        return;
                    }

                    if(profile == null) {
                        profile = Profile.createProfile(player);
                    }
                    profile.setLastLogin(Timestamp.from(Instant.now()));
                    profile.setLastIP(player.getRemoteAddress().getAddress().getHostAddress());
                    profile.setLoggedIn(player.isOnlineMode());


                    Profile.getProfiles().put(player.getUniqueId(), profile);

                    player.sendMessage(Component.text("Your profile has been loaded!").color(NamedTextColor.DARK_AQUA));

                }).thenAccept((profile) -> instance.getBackendStorage().saveProfile(profile).whenComplete((saved, ex) -> {
            if(ex != null) {
                // TODO: add to queue and try again in few seconds/min or exponantial backoff. Remove from queue on logout if save was successful
                ex.printStackTrace();
                player.sendMessage(Component.text("Failed to save your profile after login. Will try in few minutes and on logout").color(NamedTextColor.RED));
            }
        }));

        // TODO: check if player with different nameCase exists. If so, kick the player!

        /*
         * TODO: captcha system -> force new users to click text on book with:
         * event.getPlayer().openBook();
         * */
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        // TODO: use event.getLoginStatus()
        final Player player = event.getPlayer();

        final Profile profile = Profile.getProfiles().get(player.getUniqueId());
        instance.getBackendStorage().saveProfile(profile)
                .whenComplete((updated, ex) -> {
                    if(ex != null) {
                        instance.getLogger().error("Saving " + player.getUsername() + " profile!", ex);
                    }
                });

        Profile.getProfiles().remove(player.getUniqueId());
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if(player.isOnlineMode()) return;

        instance.getProxy().getScheduler().buildTask(instance, () -> {
            if(!player.isActive()) return;

            player.disconnect(Component.text("You're in a cracked account. Kicked for security reasons").color(NamedTextColor.RED));
        }).delay(3, TimeUnit.SECONDS).schedule();
        /*
         * TODO: ONLY IF CRACKED:
         *  * check if lastLoggedIn < X duration AND lastRemoteAddress == currentRemoteAddress
         *      * true -> profile->loggedIn = true
         *      * false -> profile->loggedIn = false
         * */

        /*
         * TODO: ALWAYS:
         *  * if lastRemoteAddress != currentRemoteAddress -> add log
         *  * update lastRemoteAddress, lastLoggedIn, etc...
         * */
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

        if(profile.isLoggedIn()) return;

        player.sendMessage(Component.text("You may not execute this command without logging in!").color(NamedTextColor.RED));
        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe
    public void onChange(ServerPreConnectEvent event) {
        // TODO: prevent serverswitch if player is not logged in
        if(event.getOriginalServer().getServerInfo().getName().toLowerCase().contains("lobby")) return;

        Profile profile = Profile.getProfiles().get(event.getPlayer().getUniqueId());
        if(!profile.isLoggedIn()) {
            event.getPlayer().sendMessage(Component.text("You may not switch servers without logging in!").color(NamedTextColor.RED));
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

}
