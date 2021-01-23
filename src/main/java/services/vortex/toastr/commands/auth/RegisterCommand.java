package services.vortex.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.listeners.AuthListener;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.utils.HashMethods;
import services.vortex.toastr.utils.SaltGenerator;
import services.vortex.toastr.utils.StringUtils;

import java.util.concurrent.TimeUnit;

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
            player.sendMessage(Component.text("You are a premium user.").color(NamedTextColor.RED));
            return;
        }

        Profile profile = Profile.getProfiles().get(player.getUniqueId());
        if(!StringUtils.isNullOrEmpty(profile.getPassword())) {
            player.sendMessage(Component.text("already registered...").color(NamedTextColor.RED));
            return;
        }

        if(invocation.arguments().length != 2) {
            player.sendMessage(Component.text("/register <password> <password>").color(NamedTextColor.RED));
            return;
        }

        if(!invocation.arguments()[0].equals(invocation.arguments()[1])) {
            player.sendMessage(Component.text("Password and password confirmation are different!").color(NamedTextColor.RED));
            return;
        }

        String salt = SaltGenerator.generateString();

        AuthListener.pendingRegister.remove(player);
        profile.setSalt(salt);
        profile.setPassword(HashMethods.SHA512H(invocation.arguments()[0], salt));

        try {
            instance.getBackendStorage().saveProfile(profile).get(3, TimeUnit.SECONDS);
        } catch(Exception ex) {
            instance.getLogger().error("Error registering " + player.getUsername(), ex);
            player.sendMessage(Component.text("Error registering. Contact admin").color(NamedTextColor.RED));
            return;
        }

        profile.setLoggedIn(true);
        player.sendMessage(Component.text("Successfully registered! You're now logged in").color(NamedTextColor.DARK_AQUA));
    }
}
