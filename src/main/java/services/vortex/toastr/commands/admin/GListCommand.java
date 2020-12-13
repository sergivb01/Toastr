package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import services.vortex.toastr.ToastrPlugin;

import java.util.Set;

public class GListCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        if(args.length == 0) {
            source.sendMessage(instance.getConfig().getMessage("glist_global", "players", Integer.toString(instance.getRedisManager().getOnlinePlayers())));
            return;
        }

        String proxy = args[0];
        Set<String> players = instance.getCacheManager().getOnlinePlayers(proxy);
        if(players == null) {
            source.sendMessage(instance.getConfig().getMessage("glist_no_proxy"));
            return;
        }

        source.sendMessage(instance.getConfig().getMessage("glist_proxy", "proxy", proxy, "players", Integer.toString(players.size())));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.glist");
    }

}
