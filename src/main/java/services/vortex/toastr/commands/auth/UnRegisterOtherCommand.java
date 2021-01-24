package services.vortex.toastr.commands.auth;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.utils.CC;

public class UnRegisterOtherCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();
        if(args.length != 1) {
            source.sendMessage(CC.translate("&cUsage: /aunregister <username>"));
            return;
        }

        final String username = args[0];
        try {
            if(instance.getBackendStorage().unregisterPlayer(username)){
                source.sendMessage(CC.translate("&2Successfully unregistered " + username));
                return;
            }
            source.sendMessage(CC.translate("&cThe player does not exist or is premium"));
        } catch(Exception ex) {
            instance.getLogger().error("Error trying to unregister " + username, ex);
            source.sendMessage(CC.translate("Error while trying to unregister, contact admin"));
            return;
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.aunregister");
    }
}