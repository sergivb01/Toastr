package dev.sergivos.authguard.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.authguard.AuthGuard;
import dev.sergivos.authguard.resolver.Resolver;
import net.kyori.adventure.text.Component;

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
             * TODO:
             *  * check if profile exists in cache -> load
             *  * load profile from SQL database. Update values
             *  * if it doesn't exist, create a new profile
             * */

            if(result.isPremium()) {
                /*
                 * TODO: set profile->loggedIn = true
                 */
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                instance.getLogger().info(event.getUsername() + " has been forced into online mode!");
            } else {
                /*
                 * TODO:
                 *  * check if lastLoggedIn < X duration AND lastRemoteAddress == currentRemoteAddress
                 *      * true -> profile->loggedIn = true
                 *      * false -> profile->loggedIn = false
                 * */
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
                instance.getLogger().info(event.getUsername() + " is offline user!");
            }

            /*
             * TODO:
             *  * if lastRemoteAddress != currentRemoteAddress -> add log
             *  * update lastRemoteAddress, lastLoggedIn, etc...
             * */

        } catch(Exception ex) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("error in backend")));
            ex.printStackTrace();
        }
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
