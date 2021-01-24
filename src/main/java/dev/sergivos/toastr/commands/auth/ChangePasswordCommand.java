package dev.sergivos.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.profile.Profile;
import dev.sergivos.toastr.utils.HashMethods;
import dev.sergivos.toastr.utils.SaltGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ChangePasswordCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        if(player.isOnlineMode()) {
            player.sendMessage(Component.text("You are a premium user.").color(NamedTextColor.RED));
            return;
        }

        Profile profile = Profile.getProfiles().get(player.getUniqueId());
        if(!profile.isLoggedIn()) {
            player.sendMessage(Component.text("login first").color(NamedTextColor.RED));
            return;
        }

        if(invocation.arguments().length != 1) {
            player.sendMessage(Component.text("/changepassword <password>").color(NamedTextColor.RED));
            return;
        }

        String salt = SaltGenerator.generateString();

        profile.setSalt(salt);
        profile.setPassword(HashMethods.SHA512H(invocation.arguments()[0], salt));

        try {
            instance.getBackendStorage().saveProfile(profile);
        } catch(Exception ex) {
            instance.getLogger().error("Error changing password for " + player.getUsername(), ex);
            player.sendMessage(Component.text("Error changing password. Contact admin").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Successfully changed password!").color(NamedTextColor.DARK_AQUA));
    }
}
