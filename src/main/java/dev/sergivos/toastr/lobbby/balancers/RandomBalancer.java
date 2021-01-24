package dev.sergivos.toastr.lobbby.balancers;

import dev.sergivos.toastr.lobbby.Balancer;
import dev.sergivos.toastr.lobbby.Lobby;

import java.util.List;
import java.util.Random;

public class RandomBalancer implements Balancer {

    private final Random random = new Random();

    @Override
    public Lobby getLobby(List<Lobby> lobbies) {
        return lobbies.get(random.nextInt(lobbies.size()));
    }

}
