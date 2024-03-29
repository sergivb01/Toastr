package dev.sergivos.toastr.commands.admin;

import com.velocitypowered.api.command.SimpleCommand;
import dev.sergivos.toastr.ToastrPlugin;

public class ServerIDCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        invocation.source().sendMessage(instance.getMessage("server_id", "proxy", instance.getRedisManager().getProxyName()));
    }

}
