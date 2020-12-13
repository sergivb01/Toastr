package services.vortex.toastr.lobbby;

import java.util.List;

public interface Balancer {

    /**
     * This method gets the Lobby based on the selected Balancer
     * @param lobbies The list of Lobby(s)
     * @return The available Lobby, can be null
     */
    Lobby getLobby(List<Lobby> lobbies);

}
