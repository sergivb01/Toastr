package dev.sergivos.authguard.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import dev.sergivos.authguard.AuthGuard;
import dev.sergivos.authguard.resolver.Resolver;
import net.kyori.adventure.text.Component;

public class PlayerPreLogin {
    private final AuthGuard instance = AuthGuard.getInstance();

    @Subscribe
    public void onPlayerPreLogin(PreLoginEvent event) {
        try {
            Resolver.Result result = instance.getResolverManager().resolveUsername(event.getUsername());
            if(result.isPremium()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                instance.getLogger().info(event.getUsername() + " has been forced into online mode!");
                return;
            }

            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            instance.getLogger().info(event.getUsername() + " is offline user!");
        } catch(Exception ex) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("error in backend")));
            ex.printStackTrace();
        }
    }

    @Subscribe
    public void onInitialChoose(PlayerChooseInitialServerEvent event) {

    }

    @Subscribe
    public void onChange(ServerPreConnectEvent event) {
//        event.setResult(ServerPreConnectEvent.ServerResult.denied());
    }

}
