package services.vortex.toastr.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.profile.Profile;

public class LoginCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        Profile profile = Profile.getProfiles().get(player.getUniqueId());

        if(profile.isLoggedIn()) {
            player.sendMessage(Component.text("Already logged in.").color(NamedTextColor.RED));
            return;
        }

        if(profile.getPassword() == null) {
            player.sendMessage(Component.text("register first...").color(NamedTextColor.RED));
            return;
        }

        if(invocation.arguments().length != 1) {
            player.sendMessage(Component.text("/login <password>").color(NamedTextColor.RED));
            return;
        }

        if(!profile.getPassword().equals(invocation.arguments()[0])) {
            player.sendMessage(Component.text("invalid password").color(NamedTextColor.RED));
            return;
        }

        profile.setLoggedIn(true);
        player.sendMessage(Component.text("logged in success").color(NamedTextColor.DARK_AQUA));
    }
}
