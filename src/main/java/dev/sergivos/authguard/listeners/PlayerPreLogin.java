package dev.sergivos.authguard.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.authguard.AuthGuard;
import dev.sergivos.authguard.resolver.Resolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.TimeUnit;

public class PlayerPreLogin {
    private final AuthGuard instance = AuthGuard.getInstance();

    @Subscribe
    public void onPlayerPreLogin(PreLoginEvent event) {
        try {
            Resolver.Result result = instance.getResolverManager().resolveUsername(event.getUsername());
            if(result.isSpoofed()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Spoof #1 detected")));
                return;
            }

            /*
             * TODO: set profile->loggedIn = false.
             *  We'll need to check if the user was previously cracked. (username exists but not with this UUID)
             *
             * Offload PreLoginEvent and load LoginEvent (loadProfile) and PostLoginEvent(update stuff)
             */

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
        /*
         * TODO: captcha system -> force users to click text on book with:
         * event.getPlayer().openBook();
         * */

        /*
         * TODO:
         *  * check if profile exists in cache -> load
         *  * load profile from SQL database. Update values
         *  * if it doesn't exist, create a new profile
         * */
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
    public void onPostConnect(GameProfileRequestEvent event) {
        // TODO: check for possible spoofing, or PostLoginEvent
    }

    @Subscribe
    public void onInitialChoose(PlayerChooseInitialServerEvent event) {
        // TODO: send to auth servers?
    }

    @Subscribe
    public void onPlayerCommand(CommandExecuteEvent event) {
        if(!(event.getCommandSource() instanceof Player)) return;

        // TODO: prevent commands if player is not logged in
    }

    @Subscribe
    public void onChange(ServerPreConnectEvent event) {
        // TODO: prevent serverswitch if player is not logged in
    }

}
