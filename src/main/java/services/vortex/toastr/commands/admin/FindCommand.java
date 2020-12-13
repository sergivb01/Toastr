package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;

public class FindCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        if(args.length == 0) {
            source.sendMessage(instance.getConfig().getMessage("find_usage"));
            return;
        }

        PlayerData data = instance.getCacheManager().getPlayerData(args[0]);
        if(data == null) {
            source.sendMessage(instance.getConfig().getMessage("find_player_not_found"));
            return;
        }

        if(data.getServer() == null) {
            source.sendMessage(instance.getConfig().getMessage("find_player_not_found"));
            return;
        }

        source.sendMessage(instance.getConfig().getMessage("find_found", "server", data.getServer(), "proxy", data.getProxy()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.find");
    }
}
