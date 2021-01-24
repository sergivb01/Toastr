package services.vortex.toastr.lobbby.balancers;


import services.vortex.toastr.lobbby.Balancer;
import services.vortex.toastr.lobbby.Lobby;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SequentialBalancer implements Balancer {

    private final AtomicInteger lastServer = new AtomicInteger(0);

    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        int i = lastServer.incrementAndGet();
        if(i >= lobbies.size()) {
            lastServer.set(0);
            i = 0;
        }

        return lobbies.get(i);
    }

}
