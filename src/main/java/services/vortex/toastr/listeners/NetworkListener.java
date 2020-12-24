package services.vortex.toastr.listeners;

import com.minexd.pidgin.packet.handler.IncomingPacketHandler;
import com.minexd.pidgin.packet.listener.PacketListener;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.*;

public class NetworkListener implements PacketListener {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    // TODO: broadcast packets to staff only

    @Subscribe(order = PostOrder.LAST)
    public void onInitialServerSelect(PlayerChooseInitialServerEvent event) {
        if(!event.getInitialServer().isPresent()) return;

        final Player player = event.getPlayer();
        if(!player.hasPermission("toastr.utils.staff")) return;

        instance.getRedisManager().getPidgin().sendPacket(new StaffJoinPacket(player.getUsername(), event.getInitialServer().get().getServerInfo().getName()));
    }

    @Subscribe
    public void onStaffQuit(DisconnectEvent event) {
        final Player player = event.getPlayer();

        if(!player.hasPermission("toastr.utils.staff") || !player.getCurrentServer().isPresent()) return;

        instance.getRedisManager().getPidgin().sendPacket(new StaffQuitPacket(player.getUsername(), player.getCurrentServer().get().getServerInfo().getName()));
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        if(!event.getPreviousServer().isPresent()) return;

        final Player player = event.getPlayer();
        if(!player.hasPermission("toastr.utils.staff")) return;

        instance.getRedisManager().getPidgin().sendPacket(new StaffSwitchPacket(player.getUsername(), event.getPreviousServer().get().getServerInfo().getName(), event.getServer().getServerInfo().getName()));
    }

    @IncomingPacketHandler
    public void onAlert(AlertPacket packet) {
        final Component alert = instance.getConfig().getMessage("alert")
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(packet.getMessage()));
        instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(alert));
        instance.getProxy().getConsoleCommandSource().sendMessage(alert);

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

            instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Sending message from " + packet.getSender() + " to " + packet.getReceiver() + ": " + packet.getMessage());
        });
    }

    @IncomingPacketHandler
    public void onKick(KickPacket packet) {
        instance.getProxy().getPlayer(packet.getUsername()).ifPresent(player -> {
            final TextComponent reason = Component.text("Cross network kick requested from " + packet.getOrigin() + ":")
                    .color(NamedTextColor.RED)
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(packet.getReason()));
            player.disconnect(reason);
        });

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Requested cross-network kick for " + packet.getUsername() + ": " + packet.getReason());
    }

    @IncomingPacketHandler
    public void onNetworkStatus(NetworkStatusPacket packet) {
        instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(Component.text(packet.toString())));

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Proxy " + packet.getOrigin() + " is now " + (packet.isUp() ? "online" : "offline"));
    }

    @IncomingPacketHandler
    public void onStaffJoin(StaffJoinPacket packet) {
        instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(Component.text(packet.toString())));

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Staff " + packet.getPlayer() + " joined the network");
    }

    @IncomingPacketHandler
    public void onStaffQuit(StaffQuitPacket packet) {
        instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(Component.text(packet.toString())));

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Staff " + packet.getPlayer() + " quit the network");
    }

    @IncomingPacketHandler
    public void onStaffSwitch(StaffSwitchPacket packet) {
        instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(Component.text(packet.toString())));

        instance.getLogger().info("[packet] [" + packet.getOrigin() + "] Staff " + packet.getPlayer() + " switched from " + packet.getFrom() + " to " + packet.getTo());
    }

}
