package services.vortex.toastr.lobbby;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import services.vortex.toastr.ToastrPlugin;

@RequiredArgsConstructor
@Getter
public class Lobby {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    private final String name;
    private final boolean restricted;
    private final RegisteredServer server;

    @Setter
    private int maxPlayers = -1;

    public int getOnline() {
        return instance.getRedisManager().getServerCount(server.getServerInfo().getName());
    }

}
