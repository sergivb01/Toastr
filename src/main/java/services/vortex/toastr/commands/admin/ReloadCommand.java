package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;

public class ReloadCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(!instance.loadConfig() || instance.getConfig() == null) {
            invocation.source().sendMessage(Component.text("Error reloading, check console!").color(NamedTextColor.RED));
            return;
        }
        instance.getLobbyManager().loadLobbies();
        invocation.source().sendMessage(Component.text("Reloaded!").color(NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.reload");
    }
}
