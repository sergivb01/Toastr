package dev.sergivos.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.listeners.AuthListener;
import dev.sergivos.toastr.profile.Profile;
import dev.sergivos.toastr.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class UnRegisterCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        if(!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        if(player.isOnlineMode()) {
            player.sendMessage(CC.translate("&7[&e⚠&7] &cThis command is intended for players with a non-Premium account."));
            return;
        }

        Profile profile = Profile.getProfiles().get(player.getUniqueId());
        if(!profile.isLoggedIn()) {
            player.sendMessage(CC.translate("&7[&e⚠&7] &cYou need to login into your account in order to unregister yourself."));
            return;
        }

        profile.setSalt(null);
        profile.setPassword(null);

        try {
            instance.getBackendStorage().saveProfile(profile);
        } catch(Exception ex) {
            instance.getLogger().error("Error un-registering " + player.getUsername(), ex);
            player.sendMessage(CC.translate("&7[&e⚠&7] &cAn error occurred while trying to unregister your account, please contact an administrator."));
            return;
        }

        AuthListener.pendingRegister.put(player, System.currentTimeMillis());
        player.sendMessage(CC.translate("&aYour account has been successfully unregistered. You may now register again."));
    }
}