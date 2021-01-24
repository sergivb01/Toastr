package dev.sergivos.toastr.commands.admin;

import com.velocitypowered.api.command.RawCommand;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.types.AlertPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class AlertCommand implements RawCommand {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(invocation.arguments().isEmpty()) {
            invocation.source().sendMessage(Component.text("/alert <message>").color(NamedTextColor.RED));
            return;
        }

        instance.getRedisManager().getPidgin().sendPacket(new AlertPacket(invocation.arguments()));
        invocation.source().sendMessage(Component.text("Sent alert to all proxy instances!").color(NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.alert");
    }
}
