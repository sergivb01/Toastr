package services.vortex.toastr.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.lobbby.Lobby;

import java.util.Optional;

public class LobbyListener {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Subscribe
    public void onInitialServerSelect(PlayerChooseInitialServerEvent event) {
        // TODO: fix won't send to restricted because players don't have any permissions yet
        Lobby lobby = instance.getLobbyManager().getLobby(event.getPlayer());
        if(lobby == null) {
            event.getPlayer().disconnect(Component.text("No lobby available").color(NamedTextColor.RED));
            return;
        }

        event.setInitialServer(lobby.getServer());
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        if(!instance.getLobbyManager().isSendPlayerToHubOnClose())
            return;

        final Optional<Component> kickReason = event.getServerKickReason();
        if(!kickReason.isPresent())
            return;

        String json = GsonComponentSerializer.gson().serialize(kickReason.get());
        if(!json.toLowerCase().contains("server closed"))
            return;

        Lobby lobby = instance.getLobbyManager().getLobby(event.getPlayer());
        if(lobby == null) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(instance.getConfig().getMessage("kickNoLobbyAvailable")));
            return;
        }

        event.setResult(KickedFromServerEvent.RedirectPlayer.create(lobby.getServer()));
    }

}
