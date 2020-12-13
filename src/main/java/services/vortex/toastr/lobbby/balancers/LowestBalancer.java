package services.vortex.toastr.lobbby.balancers;


import lombok.RequiredArgsConstructor;
import services.vortex.toastr.lobbby.Balancer;
import services.vortex.toastr.lobbby.Lobby;

import java.util.List;

@RequiredArgsConstructor
public class LowestBalancer implements Balancer {

    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        Lobby lowestLobby = null;

        for(Lobby lobby : lobbies) {
            if(lowestLobby == null || lowestLobby.getServer().getPlayersConnected().size() > lobby.getServer().getPlayersConnected().size())
                lowestLobby = lobby;
        }

        return lowestLobby;
    }

}
