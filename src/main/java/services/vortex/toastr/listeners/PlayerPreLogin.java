package services.vortex.toastr.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.resolver.Resolver;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class PlayerPreLogin {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Subscribe
    public void onPlayerPreLogin(PreLoginEvent event) {
        try {
            Resolver.Result result = instance.getResolverManager().resolveUsername(event.getUsername());
            if(result.isSpoofed()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Spoof #1 detected")));
                return;
            }

            if(result.isPremium()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                instance.getLogger().info(event.getUsername() + " has been forced into online mode!");
            } else {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
                instance.getLogger().info(event.getUsername() + " has been forced into offline mode!");
            }
        } catch(Exception ex) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("error in backend")));
            ex.printStackTrace();
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();

        long start = System.currentTimeMillis();

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
                    profile.setLastIP(player.getRemoteAddress().getHostName());
                    profile.setLoggedIn(player.isOnlineMode());

                    long took = System.currentTimeMillis() - start;

                    instance.getLogger().info("GetProfile for " + player.getUsername() + " took " + took + "ms");
                    instance.getLogger().info(profile.toString());

                    player.sendMessage(Component.text("Your profile has been loaded in " + took + "ms!").color(NamedTextColor.DARK_AQUA)
                            .append(Component.text("\n\n" + profile.toString()).color(NamedTextColor.WHITE)));
                    Profile.getProfiles().put(player.getUniqueId(), profile);

                }).thenAccept((profile) -> instance.getBackendStorage().savePlayer(profile).whenComplete((saved, ex) -> {
            if(ex != null) {
                // TODO: add to queue and try again in few seconds/min or exponantial backoff. Remove from queue on logout if save was successful
                ex.printStackTrace();
                player.sendMessage(Component.text("Failed to save your profile after login. Will try in few minutes and on logout").color(NamedTextColor.RED));
                return;
            }

            instance.getLogger().info("Saved " + player.getUsername() + " in " + (System.currentTimeMillis() - start) + "ms");
        }));


        /*
         * TODO: captcha system -> force new users to click text on book with:
         * event.getPlayer().openBook();
         * */
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        // TODO: use event.getLoginStatus()

        final long start = System.currentTimeMillis();
        final Player player = event.getPlayer();

        final Profile profile = Profile.getProfiles().get(player.getUniqueId());
        instance.getBackendStorage().savePlayer(profile)
                .whenComplete((updated, ex) -> {
                    if(ex != null) {
                        instance.getLogger().error("saving " + player.getUsername() + " profile");
                        ex.printStackTrace();
                        return;
                    }

                    instance.getLogger().info("Saved " + player.getUsername() + " in " + (System.currentTimeMillis() - start) + "ms");
                });

        Profile.getProfiles().remove(player.getUniqueId());
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        TextComponent header = Component.text("Vortex Services")
                .color(NamedTextColor.DARK_AQUA);
        TextComponent footer = Component.text("Development Proxy")
                .color(NamedTextColor.DARK_RED);

        player.getTabList().setHeaderAndFooter(header, footer);

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
    public void onPostConnect(GameProfileRequestEvent event) {
        // TODO: check for possible spoofing, or PostLoginEvent
    }

    @Subscribe
    public void onPlayerCommand(CommandExecuteEvent event) {
        if(!(event.getCommandSource() instanceof Player))
            return;

        final JsonArray allowedCommands = instance.getConfig().getObject().get("auth").getAsJsonObject().get("allowed_commands").getAsJsonArray();
        if(!allowedCommands.contains(JsonParser.parseString(event.getCommand().split(" ")[0].replace("/", ""))))
            return;

        Player player = (Player) event.getCommandSource();
        Profile profile = Profile.getProfiles().get(player.getUniqueId());

        if(profile.isLoggedIn()) return;

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

    @Subscribe
    public void onPostChange(ServerConnectedEvent event) {
        if(event.getServer() == null) return;

        // TODO: https://hasteb.in/hurokuhi.cs
        Profile.getProfiles().get(event.getPlayer().getUniqueId()).setLastServer(event.getServer().getServerInfo().getName());
    }

}
