package dev.sergivos.toastr.lobbby.balancers;


import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.lobbby.Balancer;
import dev.sergivos.toastr.lobbby.Lobby;

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
