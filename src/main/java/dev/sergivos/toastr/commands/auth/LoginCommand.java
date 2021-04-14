package dev.sergivos.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.listeners.AuthListener;
import dev.sergivos.toastr.profile.Profile;
import dev.sergivos.toastr.utils.CC;
import dev.sergivos.toastr.utils.HashMethods;
import dev.sergivos.toastr.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LoginCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        Profile profile = Profile.getProfiles().get(player.getUniqueId());

        if(profile.isLoggedIn()) {
            player.sendMessage(CC.translate("&7[&e⚠&7] &cYou are already logged in."));
            return;
        }

        if(StringUtils.isNullOrEmpty(profile.getPassword())) {
            player.sendMessage(CC.translate("&7[&e⚠&7] &cYou need to complete your registration prior login in."));
            return;
        }

        if(invocation.arguments().length != 1) {
            player.sendMessage(Component.text("/login <password>").color(NamedTextColor.RED));
            return;
        }

        if(!profile.getPassword().equals(HashMethods.SHA512H(invocation.arguments()[0], profile.getSalt()))) {
            player.sendMessage(CC.translate("&7[&e⚠&7] &cThe password you have entered does not match the one in the registration. Please try again or contact an administrator."));
            return;
        }

        AuthListener.pendingLogin.remove(player);
        profile.setLoggedIn(true);
        player.sendMessage(CC.translate("&aYou have successfully logged in."));
    }
}
