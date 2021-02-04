package dev.sergivos.toastr.commands.essentials;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.profile.PlayerData;
import net.kyori.adventure.text.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class ProfileCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    private static final Pattern UUID_PATTERN = Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        if(args.length == 0) {
            source.sendMessage(instance.getMessage("profile_usage"));
            return;
        }

        PlayerData data;
        if(UUID_PATTERN.matcher(args[0]).matches()) {
            data = instance.getCacheManager().getPlayerData(UUID.fromString(args[0]));
            instance.getLogger().warn("uuid");
        } else {
            data = instance.getCacheManager().getPlayerData(args[0]);
            instance.getLogger().warn("username");
        }

        if(data == null) {
            source.sendMessage(instance.getMessage("profile_player_not_found"));
            return;
        }

        String lastOnline = data.getLastOnline() == 0 ? "Online" : format.format(new Date(data.getLastOnline()));
        String ip = invocation.source().hasPermission("toastr.command.tprofile.viewip") ? data.getIp() : "private";
        for(Component info : instance.getMessages("profile_player_info", "uuid", data.getUuid().toString(), "username", data.getUsername(), "lastonline", lastOnline, "ip", ip, "proxy", data.getProxy(), "server", data.getServer())) {
            source.sendMessage(info);
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        instance.getProxy().getScheduler().buildTask(instance, () -> {
            List<String> res = new ArrayList<>();
            for(String online : instance.getCacheManager().getUsernamesOnline()) {
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
