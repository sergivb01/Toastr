package dev.sergivos.toastr.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.handler.IncomingPacketHandler;
import dev.sergivos.toastr.backend.packets.listener.PacketListener;
import dev.sergivos.toastr.backend.packets.types.*;
import dev.sergivos.toastr.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class NetworkListener implements PacketListener {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final Set<Player> staff = new CopyOnWriteArraySet<>();

    @Subscribe(order = PostOrder.LAST)
    public void onInitialServerSelect(PlayerChooseInitialServerEvent event) {
        final Player player = event.getPlayer();
        final RegisteredServer server = event.getInitialServer().orElse(null);

        staff.add(player);

        if(!player.hasPermission("toastr.utils.staff")) return;

        instance.getRedisManager().getPidgin().sendPacket(new StaffJoinPacket(player.getUsername(), server == null ?
                "Unknown" : server.getServerInfo().getName()));
    }

    @Subscribe
    public void onStaffQuit(DisconnectEvent event) {
        final Player player = event.getPlayer();
        final ServerConnection server = player.getCurrentServer().orElse(null);

        staff.remove(player);

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
                .append(CC.translate(packet.getMessage()));
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
            final Component message = CC.translate("&8[&b&l" + packet.getOrigin() + "&8] &3" + packet.getSender() + " &6-> &3You&r: ");
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
                    .append(CC.translate(packet.getReason()));
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
        instance.getProxy().getConsoleCommandSource().sendMessage(component);
        staff.forEach(player -> player.sendMessage(component));
    }

}
