package services.vortex.toastr.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import services.vortex.toastr.ToastrPlugin;

import java.util.concurrent.TimeUnit;

public class PlayerListener {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    public PlayerListener() {
        instance.getProxy().getScheduler().buildTask(instance, () -> {

            Component header, footer;
            for(Player player : instance.getProxy().getAllPlayers()) {
                String server = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "unknown";
                header = instance.getConfig().getMessage("header", "proxy", instance.getRedisManager().getProxyName(), "players", Integer.toString(instance.getRedisManager().getOnlinePlayers()), "server", server);
                footer = instance.getConfig().getMessage("footer", "proxy", instance.getRedisManager().getProxyName(), "players", Integer.toString(instance.getRedisManager().getOnlinePlayers()), "server", server);

                player.getTabList().setHeaderAndFooter(header, footer);
            }
        }).repeat(3, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        instance.getRedisManager().createPlayer(player.getUniqueId(), player.getUsername(), player.getRemoteAddress().getHostName());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        instance.getRedisManager().setPlayerServer(player.getUniqueId(), event.getServer().getServerInfo().getName());
        //Profile.getProfiles().get(event.getPlayer().getUniqueId()).setLastServer(event.getServer().getServerInfo().getName());
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();
        instance.getRedisManager().cleanPlayer(player.getUniqueId());
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();

        ping.asBuilder().onlinePlayers(instance.getRedisManager().getOnlinePlayers());
    }

}
