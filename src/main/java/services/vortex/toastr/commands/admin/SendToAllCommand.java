package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.RawCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.redis.RedisManager;

public class SendToAllCommand implements RawCommand {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(invocation.arguments().isEmpty()) {
            invocation.source().sendMessage(Component.text("/sendtoall <message>").color(NamedTextColor.RED));
            return;
        }

        instance.getRedisManager().publishMessage(RedisManager.CHANNEL_SENDTOALL, invocation.arguments());
        invocation.source().sendMessage(Component.text("Sent command to all proxy instances!").color(NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.sendtoall");
    }
}
