package dev.sergivos.toastr.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.handler.IncomingPacketHandler;
import dev.sergivos.toastr.backend.packets.listener.PacketListener;
import dev.sergivos.toastr.backend.packets.types.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class NetworkListener implements PacketListener {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Subscribe(order = PostOrder.LAST)
    public void onInitialServerSelect(PostLoginEvent event) {
        final Player player = event.getPlayer();
        final ServerConnection server = player.getCurrentServer().orElse(null);

        if(!player.hasPermission("toastr.utils.staff")) return;

        instance.getRedisManager().getPidgin().sendPacket(new StaffJoinPacket(player.getUsername(), server == null ?
                "Unknown" : server.getServerInfo().getName()));
    }

    @Subscribe
    public void onStaffQuit(DisconnectEvent event) {
        final Player player = event.getPlayer();
        final ServerConnection server = player.getCurrentServer().orElse(null);

        if(!player.hasPermission("toastr.utils.staff")) return;

        instance.getRedisManager().getPidgin().sendPacket(new StaffQuitPacket(player.getUsername(), server == null ?
                "Unknown" : server.getServerInfo().getName()));
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        if(!event.getPreviousServer().isPresent()) return;

        final Player player = event.getPlayer();
        if(!player.hasPermission("toastr.utils.staff")) return;

        instance.getRedisManager().getPidgin().sendPacket(new StaffSwitchPacket(player.getUsername(),
                event.getPreviousServer().get().getServerInfo().getName(), event.getServer().getServerInfo().getName()));
    }

    @IncomingPacketHandler
    public void onAlert(AlertPacket packet) {
        final Component alert = instance.getMessage("alert")
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(packet.getMessage()));
        instance.getProxy().sendMessage(alert);

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Sent alert: " + packet.getMessage());
    }

    @IncomingPacketHandler
    public void onClearCache(ClearCachePacket packet) {
        instance.getRedisManager().clearCache(packet.getPlayer());
        instance.getCacheManager().clearCache(packet.getPlayer());

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Requested clearcache for " + packet.getPlayer());
    }

    @IncomingPacketHandler
    public void onCommand(CommandPacket packet) {
        instance.getProxy().getCommandManager().executeImmediatelyAsync(instance.getProxy().getConsoleCommandSource(), packet.getCommand());

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Executing command: " + packet.getCommand());
    }

    @IncomingPacketHandler
    public void onGlobalMessage(GlobalMessagePacket packet) {
        instance.getProxy().getPlayer(packet.getReceiver()).ifPresent(target -> {
            final TextComponent message = LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&b&l" + packet.getOrigin() + "&8] &3" + packet.getSender() + " &6-> &3You&r: ");
            target.sendMessage(message.append(Component.text(packet.getMessage())));

            instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Sending message from " + packet.getSender() +
                    " to " + packet.getReceiver() + ": " + packet.getMessage());
        });
    }

    @IncomingPacketHandler
    public void onKick(KickPacket packet) {
        instance.getProxy().getPlayer(packet.getUsername()).ifPresent(player -> {
            final TextComponent reason = Component.text("Cross network kick requested from " + packet.getOrigin() + ":\n")
                    .color(NamedTextColor.RED)
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(packet.getReason()));
            player.disconnect(reason);
        });

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Requested cross-network kick for " +
                packet.getUsername() + ": " + packet.getReason());
    }

    @IncomingPacketHandler
    public void onNetworkStatus(NetworkStatusPacket packet) {
        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Proxy " + packet.getOrigin() + " is now " +
                (packet.isUp() ? "online" : "offline"));
    }

    @IncomingPacketHandler
    public void onStaffJoin(StaffJoinPacket packet) {
        broadcastStaff(instance.getMessage("staff.join", "player", packet.getPlayer(),
                "server", packet.getServer(), "proxy", packet.getOrigin()));

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Staff " + packet.getPlayer() + " joined the network");
    }

    @IncomingPacketHandler
    public void onStaffQuit(StaffQuitPacket packet) {
        broadcastStaff(instance.getMessage("staff.quit", "player", packet.getPlayer(),
                "server", packet.getServer(), "proxy", packet.getOrigin()));

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Staff " + packet.getPlayer() + " quit the network");
    }

    @IncomingPacketHandler
    public void onStaffSwitch(StaffSwitchPacket packet) {
        broadcastStaff(instance.getMessage("staff.switch", "player", packet.getPlayer(),
                "proxy", packet.getOrigin(), "from", packet.getFrom(), "to", packet.getTo(), "proxy", packet.getOrigin()));

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Staff " + packet.getPlayer() + " switched from " +
                packet.getFrom() + " to " + packet.getTo());
    }

    private void broadcastStaff(Component component) {
        for(Player player : instance.getProxy().getAllPlayers()) {
            if(player.hasPermission("toastr.utils.staff"))
                player.sendMessage(component);
        }
        instance.getProxy().getConsoleCommandSource().sendMessage(component);
    }

}
