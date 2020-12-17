package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;

import java.io.FileNotFoundException;

public class ReloadCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        try {
            instance.getConfig().reload();
            instance.getLobbyManager().loadLobbies();

            invocation.source().sendMessage(Component.text("Reloaded!").color(NamedTextColor.GREEN));
        } catch(FileNotFoundException e) {
            instance.getLogger().error("Error trying to reload config!", e);
            invocation.source().sendMessage(Component.text("Error reloading, check console!").color(NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.reload");
    }
}
