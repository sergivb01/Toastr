package dev.sergivos.toastr.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.types.KickPacket;
import dev.sergivos.toastr.profile.PlayerData;
import dev.sergivos.toastr.tasks.UpdateTabTask;
import dev.sergivos.toastr.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class PlayerListener {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    public PlayerListener() {
        instance.getProxy().getScheduler().buildTask(instance, new UpdateTabTask())
                .repeat(1, TimeUnit.SECONDS)
                .schedule();
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        final InetSocketAddress address = player.getRemoteAddress();
        if(address == null || address.isUnresolved()) {
            player.disconnect(CC.translate("&cNull address detected\nContact administrator or try again later"));
            instance.getLogger().warn("Player {} with UUID {} tried to join with a null or unresolved address", player.getUsername(), player.getUniqueId().toString());
            return;
        }

        final PlayerData playerData = instance.getRedisManager().getPlayer(player.getUniqueId());
        if(playerData != null && playerData.getLastOnline() == 0) {
            player.disconnect(Component.text("Player already online in the network, requested cross-network kick.\nPlease relog in").color(NamedTextColor.RED));
            instance.getRedisManager().getPidgin().sendPacket(new KickPacket(player.getUsername(), "&cYou have logged in from another location, contact an admin if the issue persists"));
            instance.getRedisManager().cleanPlayer(player.getUniqueId(), playerData.getUsername());
            return;
        }

        instance.getRedisManager().createPlayer(player.getUniqueId(), player.getUsername(), address.getAddress().getHostAddress());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        instance.getRedisManager().setPlayerServer(player.getUniqueId(), event.getServer().getServerInfo().getName());
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();
        instance.getRedisManager().cleanPlayer(player.getUniqueId(), player.getUsername());
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();

        final Component motd = instance.getMessage("motd.line1").append(Component.text("\n")).append(instance.getMessage("motd.line2"));

        event.setPing(ping.asBuilder()
                .description(motd)
                .onlinePlayers(instance.getRedisManager().getOnlinePlayers().get())
                .build());
    }

}
