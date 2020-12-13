package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LastSeenCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        if(args.length == 0) {
            source.sendMessage(instance.getConfig().getMessage("last_seen_usage"));
            return;
        }

        PlayerData data = instance.getCacheManager().getPlayerData(args[0]);
        if(data == null) {
            source.sendMessage(instance.getConfig().getMessage("last_seen_player_not_found"));
            return;
        }

        if(data.getLastOnline() == 0) {
            source.sendMessage(instance.getConfig().getMessage("last_seen_already_online"));
            source.sendMessage(Component.text(data.toString()));
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(data.getLastOnline());
        source.sendMessage(instance.getConfig().getMessage("last_seen_time", "time", format.format(date)));
        source.sendMessage(Component.text(data.toString()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.lastseen");
    }

}
