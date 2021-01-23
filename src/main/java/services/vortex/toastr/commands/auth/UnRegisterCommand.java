package services.vortex.toastr.commands.auth;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.listeners.AuthListener;
import services.vortex.toastr.profile.Profile;
import services.vortex.toastr.utils.StringUtils;

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
            player.sendMessage(Component.text("You are a premium user.").color(NamedTextColor.RED));
            return;
        }

        Profile profile = Profile.getProfiles().get(player.getUniqueId());
        if(!profile.isLoggedIn()) {
            player.sendMessage(Component.text("login first").color(NamedTextColor.RED));
            return;
        }

        if(StringUtils.isNullOrEmpty(profile.getPassword())) {
            player.sendMessage(Component.text("not registered...").color(NamedTextColor.RED));
            return;
        }

        profile.setSalt(null);
        profile.setPassword(null);

        instance.getBackendStorage().saveProfile(profile).whenComplete((saved, ex) -> {
            if(ex != null) {
                ex.printStackTrace();
                player.sendMessage(Component.text("Error un-registering. Contact admin").color(NamedTextColor.RED));
                return;
            }

            AuthListener.pendingRegister.put(player, System.currentTimeMillis());
            player.sendMessage(Component.text("Successfully un-registered!").color(NamedTextColor.DARK_AQUA));
        });

    }
}