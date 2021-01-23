package services.vortex.toastr.commands.auth;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.types.ClearCachePacket;
import services.vortex.toastr.backend.packets.types.KickPacket;
import services.vortex.toastr.profile.PlayerData;
import services.vortex.toastr.utils.CC;

import java.util.UUID;

public class UnRegisterOtherCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        // TODO: add support for username
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();
        if(args.length != 1) {
            source.sendMessage(CC.translate("&cUsage: /aunregister <uuid>"));
            return;
        }

        String rawUUID = args[0];
        final UUID uuid;
        try {
            uuid = UUID.fromString(rawUUID);
        } catch(Exception ex) {
            source.sendMessage(CC.translate("Invalid UUID"));
            return;
        }

        final PlayerData playerData = instance.getCacheManager().getPlayerData(uuid);
        if(playerData != null && playerData.getLastOnline() == 0) {
            instance.getRedisManager().getPidgin().sendPacket(new KickPacket(playerData.getUsername(), "&cUnregistering account"));
            instance.getRedisManager().getPidgin().sendPacket(new ClearCachePacket(playerData.getUsername()));
            source.sendMessage(CC.translate("&cThe player is currently online, a cross-network kick has been requested. Retry in few seconds."));
            return;
        }

        instance.getBackendStorage().unregister(uuid)
                .whenComplete((ignore, ex) -> {
                    if(ex != null) {
                        instance.getLogger().error("Error trying to unregister " + uuid.toString(), ex);
                        return;
                    }

                    source.sendMessage(CC.translate("&2Successfully unregistered player if existed"));
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("toastr.command.aunregister");
    }
}