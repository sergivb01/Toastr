package dev.sergivos.toastr.lobbby;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import dev.sergivos.toastr.ToastrPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Getter
public class Lobby {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    private final String name;
    private final boolean restricted;
    private final RegisteredServer server;
    private int maxPlayers = -1;

    public int getOnline() {
        return instance.getRedisManager().getServerCount(server.getServerInfo().getName());
    }

    public boolean isDown() {
        try {
            final ServerPing ping = server.ping().get(3, TimeUnit.SECONDS);
            Optional<ServerPing.Players> players = ping.getPlayers();
            if(!players.isPresent()) {
                instance.getLogger().warn("Couldn't determine the max players for " + name + ", ignoring server...");
                return true;
            }

            maxPlayers = players.get().getMax();

            return false;
        } catch(Exception ex) {
            instance.getLogger().warn("An error occurred while pinging " + name, ex);
        }
        return true;
    }

}
