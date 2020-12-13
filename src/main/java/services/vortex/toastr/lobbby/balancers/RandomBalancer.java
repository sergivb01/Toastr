package services.vortex.toastr.lobbby.balancers;

import services.vortex.toastr.lobbby.Balancer;
import services.vortex.toastr.lobbby.Lobby;

import java.util.List;
import java.util.Random;

public class RandomBalancer implements Balancer {
    
    private final Random random = new Random();
    
    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        return lobbies.get(random.nextInt(lobbies.size()));
    }
    
}
