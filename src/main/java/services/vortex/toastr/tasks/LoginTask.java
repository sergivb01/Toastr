package services.vortex.toastr.tasks;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class LoginTask implements Runnable {
    private final HashMap<Player, Long> pending;

    @Override
    public void run() {
        for(Player player : pending.keySet()) {
            Long loggedAt = pending.get(player);
            if((System.currentTimeMillis() - loggedAt) > TimeUnit.SECONDS.toMillis(30)) {
                player.disconnect(Component.text("Login time exceeded. Please try again").color(NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Login using /login <password>").color(NamedTextColor.DARK_GREEN));
            player.showTitle(Title.title(Component.text("Please login").color(NamedTextColor.DARK_AQUA),
                    Component.text("/login <password>").color(NamedTextColor.WHITE)));
        }
    }


}
