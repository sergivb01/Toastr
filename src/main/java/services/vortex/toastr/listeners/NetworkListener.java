package services.vortex.toastr.listeners;

import com.minexd.pidgin.packet.handler.IncomingPacketHandler;
import com.minexd.pidgin.packet.listener.PacketListener;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.AlertPacket;
import services.vortex.toastr.backend.packets.CommandPacket;
import services.vortex.toastr.backend.packets.GlobalMessagePacket;
import services.vortex.toastr.backend.packets.KickPacket;

import java.util.Optional;

public class NetworkListener implements PacketListener {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @IncomingPacketHandler
    public void onAlert(AlertPacket packet) {
        final Component alert = instance.getConfig().getMessage("alert")
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(packet.getMessage()));
        instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(alert));
        instance.getProxy().getConsoleCommandSource().sendMessage(alert);

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Sent alert: " + packet.getMessage());
    }

    @IncomingPacketHandler
    public void onCommand(CommandPacket packet) {
        instance.getProxy().getCommandManager().executeImmediatelyAsync(instance.getProxy().getConsoleCommandSource(), packet.getCommand());

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Executing command: " + packet.getCommand());
    }

    @IncomingPacketHandler
    public void onGlobalMessage(GlobalMessagePacket packet) {
        final Optional<Player> optionalPlayer = instance.getProxy().getPlayer(packet.getReceiver());
        if(!optionalPlayer.isPresent()) return;

        final Player target = optionalPlayer.get();
        final TextComponent message = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&b&l" + packet.getOrigin() + "&8] &3" + packet.getReceiver() + "&r: " + packet.getMessage());

        target.sendMessage(message);
    }

    @IncomingPacketHandler
    public void onPacket(KickPacket packet) {
        instance.getProxy().getPlayer(packet.getUsername()).ifPresent(player -> {
            final TextComponent reason = Component.text("Cross network kick requested from " + packet.getOrigin() + ":")
                    .color(NamedTextColor.RED)
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(packet.getReason()));
            player.disconnect(reason);
        });

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Requested cross-network kick for " + packet.getUsername() + ": " + packet.getReason());
    }

}
