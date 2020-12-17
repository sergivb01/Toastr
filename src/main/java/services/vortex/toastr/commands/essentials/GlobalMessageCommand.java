package services.vortex.toastr.commands.essentials;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.GlobalMessagePacket;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.utils.StringUtils;

import java.util.ArrayList;
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
            source.sendMessage(instance.getConfig().getMessage("profile_player_not_found"));
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
        instance.getRedisManager().getPidgin().sendPacket(new GlobalMessagePacket(sender, args[1], message));
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
        return invocation.source().hasPermission("toastr.command.gmsg");
    }
}
