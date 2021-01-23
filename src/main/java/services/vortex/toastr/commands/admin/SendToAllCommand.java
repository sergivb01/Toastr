package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.CommandPacket;

public class SendToAllCommand implements RawCommand {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();
        if(invocation.arguments().isEmpty()) {
            source.sendMessage(Component.text("/sendtoall <command>").color(NamedTextColor.RED));
            return;
        }

        final String[] args = invocation.arguments().split(" ");
        if(args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("shutdown") || args[0].equalsIgnoreCase("sendtoall")) {
            source.sendMessage(Component.text("Don't do that please :(").color(NamedTextColor.RED));
            return;
        }

        instance.getRedisManager().getPidgin().sendPacket(new CommandPacket(invocation.arguments()));
        source.sendMessage(Component.text("Sent command to all proxy instances!").color(NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.sendtoall");
    }
}
