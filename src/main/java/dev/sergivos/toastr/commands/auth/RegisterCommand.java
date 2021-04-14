package dev.sergivos.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.listeners.AuthListener;
import dev.sergivos.toastr.profile.Profile;
import dev.sergivos.toastr.utils.CC;
import dev.sergivos.toastr.utils.HashMethods;
import dev.sergivos.toastr.utils.SaltGenerator;
import dev.sergivos.toastr.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RegisterCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
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
        if(!StringUtils.isNullOrEmpty(profile.getPassword())) {
            player.sendMessage(CC.translate("&7[&e⚠&7] &cYou are already registered. If you intended to change your account you may login and change your password."));
            return;
        }

        if(invocation.arguments().length != 2) {
            player.sendMessage(Component.text("/register <password> <password>").color(NamedTextColor.RED));
            return;
        }

        if(!invocation.arguments()[0].equals(invocation.arguments()[1])) {
            player.sendMessage(CC.translate("&7[&e⚠&7] &cThe password you entered and the confirmation do not match. Please try again."));
            return;
        }

        String salt = SaltGenerator.generateString();

        profile.setSalt(salt);
        profile.setPassword(HashMethods.SHA512H(invocation.arguments()[0], salt));

        try {
            instance.getBackendStorage().saveProfile(profile);
        } catch(Exception ex) {
            instance.getLogger().error("Error registering " + player.getUsername(), ex);
            player.sendMessage(CC.translate("&7[&e⚠&7] &cAn error occurred while trying to register your account, please contact an administrator."));
            return;
        }

        AuthListener.pendingRegister.remove(player);
        profile.setLoggedIn(true);
        player.sendMessage(CC.translate("&aYour account has been successfully registered. You are now logged in."));
    }
}
