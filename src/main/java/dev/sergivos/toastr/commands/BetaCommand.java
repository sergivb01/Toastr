package dev.sergivos.toastr.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.utils.CC;

public class BetaCommand implements SimpleCommand {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(invocation.source() instanceof ConsoleCommandSource) {
            invocation.source().sendMessage(CC.translate("&conly players"));
            return;
        }

        Player player = (Player) invocation.source();
        player.sendMessage(CC.translate("You have been granted &b&l* &ron Velocity for &32 hours&f."));
        instance.getProxy().getCommandManager().executeImmediatelyAsync(instance.getProxy().getConsoleCommandSource(), "lpv user " + player.getUsername() + " permission settemp * true 2h");
    }

}
