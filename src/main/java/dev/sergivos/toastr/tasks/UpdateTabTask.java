package dev.sergivos.toastr.tasks;

import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import net.kyori.adventure.text.Component;

public class UpdateTabTask implements Runnable {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void run() {
        int online = instance.getRedisManager().getOnlinePlayers().get();

        Component header, footer;
        for(Player player : instance.getProxy().getAllPlayers()) {
            String server = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "unknown";
            header = instance.getMessage("header", "proxy", instance.getRedisManager().getProxyName(), "online", Integer.toString(online), "server", server);
            footer = instance.getMessage("footer", "proxy", instance.getRedisManager().getProxyName(), "online", Integer.toString(online), "server", server);

            player.getTabList().setHeaderAndFooter(header, footer);
        }
    }
}