package services.vortex.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.listeners.AuthListener;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.utils.HashMethods;
import services.vortex.toastr.utils.StringUtils;

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
            player.sendMessage(Component.text("Already logged in.").color(NamedTextColor.RED));
            return;
        }

        if(StringUtils.isNullOrEmpty(profile.getPassword())) {
            player.sendMessage(Component.text("register first...").color(NamedTextColor.RED));
            return;
        }

        if(invocation.arguments().length != 1) {
            player.sendMessage(Component.text("/login <password>").color(NamedTextColor.RED));
            return;
        }

        if(!profile.getPassword().equals(HashMethods.SHA512H(invocation.arguments()[0], profile.getSalt()))) {
            player.sendMessage(Component.text("invalid password").color(NamedTextColor.RED));
            return;
        }

        AuthListener.pendingLogin.remove(player);
        profile.setLoggedIn(true);
        player.sendMessage(Component.text("logged in success").color(NamedTextColor.DARK_AQUA));
    }
}
