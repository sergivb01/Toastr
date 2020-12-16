package services.vortex.toastr.commands.essentials;

import com.velocitypowered.api.command.SimpleCommand;
import services.vortex.toastr.ToastrPlugin;

public class ServerIDCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        invocation.source().sendMessage(instance.getConfig().getMessage("server_id", "proxy", instance.getRedisManager().getProxyName()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.serverid");
    }

}
