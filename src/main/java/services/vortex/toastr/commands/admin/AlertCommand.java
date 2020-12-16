package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.RawCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.redis.RedisManager;

public class AlertCommand implements RawCommand {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(invocation.arguments().isEmpty()) {
            invocation.source().sendMessage(Component.text("/alert <message>").color(NamedTextColor.RED));
            return;
        }

        if(instance.isMultiInstance()) {
            instance.getRedisManager().publishMessage(RedisManager.CHANNEL_ALERT, invocation.arguments());
            invocation.source().sendMessage(Component.text("Sent alert to all proxy instances!").color(NamedTextColor.GREEN));
        }else{
            final Component alert = instance.getConfig().getMessage("alert")
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(invocation.arguments()));
            instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(alert));
            instance.getProxy().getConsoleCommandSource().sendMessage(alert);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.alert");
    }
}
