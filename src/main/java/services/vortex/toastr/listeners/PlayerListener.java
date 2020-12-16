package services.vortex.toastr.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.KickPacket;
import services.vortex.toastr.profile.PlayerData;

import java.util.concurrent.TimeUnit;

public class PlayerListener {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    public PlayerListener() {
        instance.getProxy().getScheduler().buildTask(instance, () -> {

            Component header, footer;
            for(Player player : instance.getProxy().getAllPlayers()) {
                String server = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "unknown";
                header = instance.getConfig().getMessage("header", "proxy", instance.getRedisManager().getProxyName(), "online", Integer.toString(instance.getRedisManager().getOnlinePlayers()), "server", server);
                footer = instance.getConfig().getMessage("footer", "proxy", instance.getRedisManager().getProxyName(), "online", Integer.toString(instance.getRedisManager().getOnlinePlayers()), "server", server);

                player.getTabList().setHeaderAndFooter(header, footer);
            }
        }).repeat(3, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        final PlayerData playerData = instance.getRedisManager().getPlayer(player.getUniqueId());
        if(playerData != null && playerData.getLastOnline() == 0) {
            player.disconnect(Component.text("Player already online in the network, requested cross-network kick.\nPlease relog in").color(NamedTextColor.RED));
            instance.getRedisManager().getPidgin().sendPacket(new KickPacket(player.getUsername(), "&cYou have logged in from another location, contact an admin if the issue persists"));
            return;
        }

        instance.getRedisManager().createPlayer(player.getUniqueId(), player.getUsername(), player.getRemoteAddress().getAddress().getHostAddress());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        instance.getRedisManager().setPlayerServer(player.getUniqueId(), player.getUsername(), event.getServer().getServerInfo().getName());
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();
        instance.getRedisManager().cleanPlayer(player.getUniqueId(), player.getUsername());
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();

        ping.asBuilder().onlinePlayers(instance.getRedisManager().getOnlinePlayers());
    }

}
