package services.vortex.toastr.lobbby.balancers;


import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.lobbby.Balancer;
import services.vortex.toastr.lobbby.Lobby;

import java.util.List;

public class FirstAvailableBalancer implements Balancer {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        for(Lobby lobby : lobbies) {
            int onlinePlayers = lobby.getOnline();
            if(onlinePlayers < lobby.getMaxPlayers())
                return lobby;
        }

        return null;
    }

}
