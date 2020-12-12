package services.vortex.toastr.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.Profile;

public class RegisterCommand implements SimpleCommand {

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

        if(profile.getPassword() != null) {
            player.sendMessage(Component.text("already registered...").color(NamedTextColor.RED));
            return;
        }

        if(invocation.arguments().length != 1) {
            player.sendMessage(Component.text("/register <password>").color(NamedTextColor.RED));
            return;
        }

        profile.setPassword(invocation.arguments()[0]);
        ToastrPlugin.getInstance().getBackendStorage().savePlayer(profile).whenComplete((saved, ex) -> {
            if(ex != null) {
                ex.printStackTrace();
                player.sendMessage(Component.text("Error registering. Contact admin").color(NamedTextColor.RED));
                return;
            }

            player.sendMessage(Component.text("Successfully registered!").color(NamedTextColor.DARK_AQUA));
        });

    }
}
