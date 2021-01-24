package dev.sergivos.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.types.ClearCachePacket;
import dev.sergivos.toastr.backend.packets.types.KickPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClearCacheCommand implements SimpleCommand {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();
        if(invocation.arguments().length != 1) {
            source.sendMessage(Component.text("/clearcache <username>"));
            return;
        }

        String username = invocation.arguments()[0];

        instance.getRedisManager().getPidgin().sendPacket(new KickPacket(username, "&cRequested clearcache"));
        instance.getRedisManager().getPidgin().sendPacket(new ClearCachePacket(username));

        source.sendMessage(Component.text("Requested clearcache on all proxy instances").color(NamedTextColor.DARK_GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.clearcache");
    }
}
