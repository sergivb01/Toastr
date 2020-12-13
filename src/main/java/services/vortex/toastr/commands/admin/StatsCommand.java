package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import services.vortex.toastr.ToastrPlugin;

public class StatsCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        source.sendMessage(Component.text(instance.getResolverManager().getStats().toString()));
    }
}
