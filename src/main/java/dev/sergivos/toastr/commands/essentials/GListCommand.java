package dev.sergivos.toastr.commands.essentials;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.sergivos.toastr.ToastrPlugin;

import java.util.Set;

// TODO: rework this mess
public class GListCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        int global = instance.getRedisManager().getOnlinePlayers().get();
        if(args.length == 0) {
            source.sendMessage(instance.getMessage("glist_global", "online", Integer.toString(global)));
            return;
        }

        String proxy = args[0];
        // TODO: reimplement this
        /*if(!proxy.equalsIgnoreCase("ALL") && !instance.getRedisManager().getKnownProxies().contains(proxy)) {
            source.sendMessage(Component.text("Unknown proxy").color(NamedTextColor.RED));
            return;
        }*/

        final Long online = instance.getRedisManager().getProxyCount(proxy);
        if(online == null || proxy.equalsIgnoreCase("ALL")) {
            for(RegisteredServer server : instance.getProxy().getAllServers()) {
                final Set<String> players = instance.getRedisManager().getServerUsernames(server.getServerInfo().getName());
                source.sendMessage(instance.getMessage("glist_per_server", "server", server.getServerInfo().getName(), "online", Long.toString(players.size()), "players", String.join(", ", players)));
            }
            // TODO: reimplement this
//            for(String knownProxy : instance.getRedisManager().getKnownProxies()) {
//                source.sendMessage(instance.getMessage("glist_per_proxy", "proxy", knownProxy, "online", Long.toString(instance.getRedisManager().getProxyCount(knownProxy))));
//            }
            source.sendMessage(instance.getMessage("glist_global", "online", Integer.toString(global)));
            return;
        }

        source.sendMessage(instance.getMessage("glist_proxy", "proxy", proxy, "online", Long.toString(online)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.glist");
    }

}
