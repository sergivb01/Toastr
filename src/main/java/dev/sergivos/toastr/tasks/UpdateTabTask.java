package dev.sergivos.toastr.tasks;

import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import net.kyori.adventure.text.Component;

public class UpdateTabTask implements Runnable {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void run() {
        // TODO: add server-count -> IMPORTANT: add cache or something to prevent looking up in redis each iteration
        int online = instance.getRedisManager().getOnlinePlayers().get();

        for(Player player : instance.getProxy().getAllPlayers()) {
            String server = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "Unknown";
            player.sendPlayerListHeaderAndFooter(getSection("header", online, server), getSection("footer", online, server));
        }
    }

    private Component getSection(final String section, Integer online, String server) {
        return instance.joinMessages("tab." + section, "proxy", instance.getRedisManager().getProxyName(),
                "online", Integer.toString(online),
                "server", server);
    }

}
