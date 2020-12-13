package services.vortex.toastr.lobbby.balancers;


import services.vortex.toastr.lobbby.Balancer;
import services.vortex.toastr.lobbby.Lobby;

import java.util.List;

public class FirstAvailableBalancer implements Balancer {

    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        for(Lobby lobby : lobbies) {
            int onlinePlayers = lobby.getServer().getPlayersConnected().size();
            if(onlinePlayers < lobby.getMaxPlayers())
                return lobby;
        }

        return null;
    }

}
