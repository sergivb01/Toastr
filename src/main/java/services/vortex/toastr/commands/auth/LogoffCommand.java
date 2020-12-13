package services.vortex.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;

public class LogoffCommand implements SimpleCommand {
    private final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        Profile profile = Profile.getProfiles().get(player.getUniqueId());

        if(profile.getAccountType() == Profile.AccountType.PREMIUM) {
            player.sendMessage(Component.text("You are a premium user.").color(NamedTextColor.RED));
            return;
        }

        if(!profile.isLoggedIn()) {
            player.sendMessage(Component.text("not logged in.").color(NamedTextColor.RED));
            return;
        }

        if(profile.getPassword() == null) {
            player.sendMessage(Component.text("register first...").color(NamedTextColor.RED));
            return;
        }

        profile.setLoggedIn(false);
        player.sendMessage(Component.text("logged off success").color(NamedTextColor.DARK_AQUA));
    }
}
