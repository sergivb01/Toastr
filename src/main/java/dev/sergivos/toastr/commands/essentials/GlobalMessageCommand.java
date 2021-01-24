package dev.sergivos.toastr.commands.essentials;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.types.GlobalMessagePacket;
import dev.sergivos.toastr.profile.PlayerData;
import dev.sergivos.toastr.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GlobalMessageCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();
        final String[] args = invocation.arguments();
        if(args.length < 2) {
            source.sendMessage(Component.text("/gmsg <player> <message>"));
            return;
        }

        PlayerData data = instance.getCacheManager().getPlayerData(args[0]);
        if(data == null) {
            source.sendMessage(instance.getMessage("profile_player_not_found"));
            return;
        }

        if(data.getLastOnline() != 0) {
            source.sendMessage(Component.text("player is not online").color(NamedTextColor.RED));
            return;
        }

        String sender = "Console";
        if(source instanceof Player) {
            sender = (((Player) source).getUsername());
        }

        String message = StringUtils.joinArray(args, " ", 2);
        instance.getRedisManager().getPidgin().sendPacket(new GlobalMessagePacket(sender, args[0], message));

        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&b&l" + instance.getRedisManager().getProxyName() + "&8] &3You &6-> &3" + args[0] + "&r: " + message));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        if(invocation.arguments().length > 1) {
            future.complete(Collections.emptyList());
            return future;
        }

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
        return invocation.source().hasPermission("toastr.command.gmsg");
    }
}
