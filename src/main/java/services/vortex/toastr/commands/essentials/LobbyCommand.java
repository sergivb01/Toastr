package services.vortex.toastr.commands.essentials;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.lobbby.Lobby;

public class LobbyCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        Lobby lobby = instance.getLobbyManager().getLobby(player);
        if(lobby == null) {
            player.sendMessage(Component.text("No lobby available").color(NamedTextColor.RED));
            return;
        }

        player.createConnectionRequest(lobby.getServer()).fireAndForget();
    }
}
