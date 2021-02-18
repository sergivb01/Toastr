package dev.sergivos.toastr.commands.essentials;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.profile.PlayerData;
import dev.sergivos.toastr.profile.Profile;
import lombok.SneakyThrows;
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

    @SneakyThrows
    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

        if(args.length == 0) {
            source.sendMessage(instance.getMessage("profile.usage"));
            return;
        }

        PlayerData data;
        if(UUID_PATTERN.matcher(args[0]).matches()) {
            data = instance.getCacheManager().getPlayerData(UUID.fromString(args[0]));
        } else {
            data = instance.getCacheManager().getPlayerData(args[0]);
        }

        if(data == null) {
            source.sendMessage(instance.getMessage("profile.player_not_found"));
            return;
        }

        Profile profile = instance.getBackendStorage().getProfile(data.getUuid());
        if(profile == null) {
            source.sendMessage(instance.getMessage("profile.player_not_found"));
            return;
        }

        String lastOnline = data.getLastOnline() == 0 ? "Online" : format.format(new Date(data.getLastOnline()));
        String firstIP = invocation.source().hasPermission("toastr.command.tprofile.viewip") ? profile.getFirstIP() : "private";
        String lastIP = invocation.source().hasPermission("toastr.command.tprofile.viewip") ? profile.getLastIP() : "private";
        for(Component info : instance.getMessages("profile.player_info",
                "uuid", data.getUuid().toString(),
                "username", data.getUsername(),
                "last_online", lastOnline,
                "proxy", data.getProxy(),
                "server", data.getServer(),
                "account_type", profile.getAccountType().toString(),
                "first_ip", firstIP,
                "last_ip", lastIP,
                "first_login", format.format(profile.getFirstLogin()),
                "last_login", format.format(profile.getLastLogin()))) {
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
