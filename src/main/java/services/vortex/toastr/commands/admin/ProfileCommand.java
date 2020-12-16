package services.vortex.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// TODO: implement lookup by UUID
public class ProfileCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        if(args.length == 0) {
            source.sendMessage(instance.getConfig().getMessage("profile_usage"));
            return;
        }

        PlayerData data = instance.getCacheManager().getPlayerData(args[0]);
        if(data == null) {
            source.sendMessage(instance.getConfig().getMessage("profile_player_not_found"));
            return;
        }

        String lastOnline = data.getLastOnline() == 0 ? "Online" : format.format(new Date(data.getLastOnline()));
        String ip = invocation.source().hasPermission("toastr.command.tprofile.viewip") ? data.getIp() : "private";
        for(Component info : instance.getConfig().getMessages("profile_player_info", "uuid", data.getUuid().toString(), "username", args[0], "lastonline", lastOnline, "ip", ip, "proxy", data.getProxy(), "server", data.getServer())) {
            source.sendMessage(info);
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        instance.getProxy().getScheduler().buildTask(instance, () -> {
            List<String> res = new ArrayList<>();
            for(String online : instance.getCacheManager().getAllOnline()) {
                if(invocation.arguments().length == 0 || online.startsWith(invocation.arguments()[0]))
                    res.add(online);
            }
            future.complete(res);
        }).schedule();

        return future;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.tprofile");
    }

}
