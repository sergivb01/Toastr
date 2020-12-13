package services.vortex.toastr.lobbby.balancers;


import services.vortex.toastr.lobbby.Balancer;
import services.vortex.toastr.lobbby.Lobby;

import java.util.List;

public class SequentialBalancer implements Balancer {

    private int lastServer = 0;

    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        lastServer++;
        if (lastServer >= lobbies.size())
            lastServer = 0;
        
        return lobbies.get(lastServer);
    }
    
}
