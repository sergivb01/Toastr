package services.vortex.toastr.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import services.vortex.toastr.ToastrPlugin;

public class StatsCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        source.sendMessage(Component.text(ToastrPlugin.getInstance().getResolverManager().getStats().toString()));
    }
}
