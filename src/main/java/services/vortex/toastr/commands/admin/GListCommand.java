package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import services.vortex.toastr.ToastrPlugin;

// TODO: rework this mess
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
        final Long players = instance.getRedisManager().getProxyCount(proxy);

        if(players == null || proxy.equalsIgnoreCase("ALL")) {
            for(RegisteredServer server : instance.getProxy().getAllServers()) {
                final int online = instance.getRedisManager().getServerCount(server.getServerInfo().getName());
                source.sendMessage(instance.getConfig().getMessage("glist_per_server", "server", server.getServerInfo().getName(), "players", Long.toString(online)));
            }
            source.sendMessage(instance.getConfig().getMessage("glist_global", "players", Integer.toString(global)));
            return;
        }

        source.sendMessage(instance.getConfig().getMessage("glist_proxy", "proxy", proxy, "players", Integer.toString(global)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.glist");
    }

}
