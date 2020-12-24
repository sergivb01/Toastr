package services.vortex.toastr.tasks;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RegisterTask implements Runnable {
    private final ConcurrentHashMap<Player, Long> pending;

    @Override
    public void run() {
        for(Map.Entry<Player, Long> entry : pending.entrySet()) {
            final Player player = entry.getKey();
            if((System.currentTimeMillis() - entry.getValue()) > TimeUnit.SECONDS.toMillis(30)) {
                player.disconnect(Component.text("Register time exceeded. Please try again").color(NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Register using /register <password> <password>").color(NamedTextColor.DARK_GREEN));
            player.showTitle(Title.title(Component.text("Please register").color(NamedTextColor.DARK_AQUA),
                    Component.text("/register <password> <password>").color(NamedTextColor.WHITE)));
        }
    }


}
