package dev.sergivos.authguard.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.sergivos.authguard.AuthGuard;
import net.kyori.adventure.text.Component;

public class StatsCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        source.sendMessage(Component.text(AuthGuard.getInstance().getResolverManager().getStats().toString()));
    }
}
