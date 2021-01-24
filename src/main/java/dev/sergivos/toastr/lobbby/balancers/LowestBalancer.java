package dev.sergivos.toastr.lobbby.balancers;


import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.lobbby.Balancer;
import dev.sergivos.toastr.lobbby.Lobby;
import lombok.RequiredArgsConstructor;

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
