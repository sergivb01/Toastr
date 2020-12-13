package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;

public class IPCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        if(args.length == 0) {
            source.sendMessage(instance.getConfig().getMessage("ip_usage"));
            return;
        }

        PlayerData data = instance.getCacheManager().getPlayerData(args[0]);
        if(data == null) {
            source.sendMessage(instance.getConfig().getMessage("ip_player_not_found"));
            return;
        }

        if(data.getIp() == null) {
            source.sendMessage(instance.getConfig().getMessage("ip_player_not_online"));
            return;
        }

        source.sendMessage(instance.getConfig().getMessage("ip_data", "ip", data.getIp()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.ip");
    }

}
