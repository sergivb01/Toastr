package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import services.vortex.toastr.ToastrPlugin;

import java.util.Set;
import java.util.UUID;

public class GListCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        int global = instance.getRedisManager().getOnlinePlayers();
        if(args.length == 0) {
            source.sendMessage(instance.getConfig().getMessage("glist_global", "players", Integer.toString(global)));
            return;
        }

        String proxy = args[0];
        Set<String> players = instance.getCacheManager().getOnlinePlayers(proxy);

        if(players == null || proxy.equalsIgnoreCase("ALL")) {
            for(RegisteredServer server : instance.getProxy().getAllServers()) {
                final Set<UUID> online = instance.getCacheManager().getOnlinePlayersInServer(server.getServerInfo().getName());
                if(online != null) {
                    source.sendMessage(instance.getConfig().getMessage("glist_per_server", "server", server.getServerInfo().getName(), "players", Integer.toString(online.size())));
                }
            }
            source.sendMessage(instance.getConfig().getMessage("glist_global", "players", Integer.toString(global)));
            return;
        }

        source.sendMessage(instance.getConfig().getMessage("glist_proxy", "proxy", proxy, "players", Integer.toString(players.size())));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.glist");
    }

}
