package services.vortex.toastr.lobbby.balancers;


import lombok.RequiredArgsConstructor;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.lobbby.Balancer;
import services.vortex.toastr.lobbby.Lobby;

import java.util.List;

@RequiredArgsConstructor
public class LowestBalancer implements Balancer {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        Lobby lowestLobby = null;
        int lowestOnline = 0;

        for(Lobby lobby : lobbies) {
            int currOnline = lobby.getOnline();
            if(lowestLobby == null || lowestOnline > currOnline) {
                lowestLobby = lobby;
                lowestOnline = currOnline;
            }
        }

        return lowestLobby;
    }

}
