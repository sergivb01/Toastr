package dev.sergivos.toastr.commands.admin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.utils.CC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SendCommand implements SimpleCommand {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();
        if(invocation.arguments().length != 2) {
            source.sendMessage(CC.translate("&cUsage: /send <player/current/all/server> <server> &4&l(NOT-NETWORK-WIDE)"));
            return;
        }

        final RegisteredServer target = instance.getProxy().getServer(invocation.arguments()[1].toLowerCase()).orElse(null);
        if(target == null) {
            source.sendMessage(CC.translate("&cInvalid target server"));
            return;
        }

        try {
            target.ping().get(3, TimeUnit.SECONDS);
        } catch(Exception ignore) {
            source.sendMessage(CC.translate("&cTarget server is offline"));
            return;
        }

        Collection<Player> playersToSend;
        switch(invocation.arguments()[0].toLowerCase()) {
            case "current": {
                if(!(source instanceof Player)) {
                    source.sendMessage(CC.translate("&cOnly players"));
                    return;
                }

                Player player = (Player) source;
                final ServerConnection current = player.getCurrentServer().orElse(null);
                if(current == null) {
                    source.sendMessage(CC.translate("&cYou need to be connected to a server"));
                    return;
                }
                playersToSend = current.getServer().getPlayersConnected();
                break;
            }

            case "all": {
                playersToSend = instance.getProxy().getAllPlayers();
                break;
            }

            default: {
                final String arg = invocation.arguments()[0];
                final Player fromPlayer = instance.getProxy().getPlayer(arg).orElse(null);
                if(fromPlayer == null) {
                    final RegisteredServer fromServer = instance.getProxy().getServer(arg).orElse(null);
                    if(fromServer == null) {
                        source.sendMessage(CC.translate("&cUnknown player or server " + arg));
                        return;
                    }
                    if(fromServer.equals(target)) {
                        source.sendMessage(CC.translate("&cCannot send the players to the same server"));
                        return;
                    }
                    playersToSend = fromServer.getPlayersConnected();
                    break;
                }
                playersToSend = Collections.singleton(fromPlayer);
                break;
            }
        }

        final String sender = source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";
        playersToSend.forEach(targetPlayer -> {
            targetPlayer.createConnectionRequest(target).fireAndForget();
            targetPlayer.sendMessage(CC.translate("&fYou have been sent to &b" + target.getServerInfo().getName() + " &fby &3" + sender));
        });
        source.sendMessage(CC.translate("You have sent &b" + playersToSend.size() + "players &rto the server &3" + target.getServerInfo().getName()));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        instance.getProxy().getScheduler().buildTask(instance, () -> {
            if(invocation.arguments().length > 2) return;

            if(invocation.arguments().length == 2) {
                future.complete(instance.getProxy().getAllServers().stream()
                        .map(server -> server.getServerInfo().getName())
                        .collect(Collectors.toList()));
                return;
            }

            final List<String> res = new ArrayList<>();
            if(invocation.arguments().length == 0) {
                res.add("all");
                res.add("current");
                res.addAll(instance.getProxy().getAllServers().stream()
                        .map(server -> server.getServerInfo().getName())
                        .collect(Collectors.toList()));
            }

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
        return invocation.source().hasPermission("toastr.commands.send");
    }
}
