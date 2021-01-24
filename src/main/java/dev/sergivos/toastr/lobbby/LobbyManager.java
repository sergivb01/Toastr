package dev.sergivos.toastr.lobbby;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.lobbby.balancers.FirstAvailableBalancer;
import dev.sergivos.toastr.lobbby.balancers.LowestBalancer;
import dev.sergivos.toastr.lobbby.balancers.RandomBalancer;
import dev.sergivos.toastr.lobbby.balancers.SequentialBalancer;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;

import java.util.*;
import java.util.stream.Collectors;

public class LobbyManager {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private List<Lobby> lobbies = Collections.synchronizedList(new ArrayList<>());
    private List<Lobby> restrictedLobbies = Collections.synchronizedList(new ArrayList<>());

    private Balancer balancer;
    @Getter
    private boolean sendPlayerToHubOnClose;

    /**
     * This method loads all the lobbies from the config file
     */
    public void loadLobbies() {
        List<Lobby> tmp = Collections.synchronizedList(new ArrayList<>());
        List<Lobby> tmp_restricted = Collections.synchronizedList(new ArrayList<>());

        final Configuration config = instance.getConfig().getSection("lobby");
        final List<Map<String, Object>> servers = (List<Map<String, Object>>) config.getList("servers");

        for(Map<String, Object> server : servers) {
            String name = (String) server.get("name");
            boolean restricted = (boolean) server.get("restricted");

            Optional<RegisteredServer> optionalServer = instance.getProxy().getServer(name);
            if(!optionalServer.isPresent()) {
                instance.getLogger().warn("Invalid server name: " + name);
                continue;
            }

            final RegisteredServer registeredServer = optionalServer.get();
            Lobby lobby = new Lobby(name, restricted, registeredServer);
            if(lobby.isDown()) {
                continue;
            }

            if(restricted) {
                tmp_restricted.add(lobby);
            } else {
                tmp.add(lobby);
            }
        }

        lobbies = tmp;
        restrictedLobbies = tmp_restricted;

        instance.getLogger().info("Loaded " + lobbies.size() + " regular lobbies.");
        instance.getLogger().info("Loaded " + restrictedLobbies.size() + " restricted lobbies.");

        balancer = getBalancer(config.getString("load-balancer"));
        sendPlayerToHubOnClose = config.getBoolean("send-on-close");
    }

    public boolean isLobby(RegisteredServer server) {
        return lobbies.stream().map(Lobby::getServer).collect(Collectors.toSet()).contains(server)
                || restrictedLobbies.stream().map(Lobby::getServer).collect(Collectors.toSet()).contains(server);
    }

    /**
     * This method gets the first available Lobby
     *
     * @return The Lobby, can be null
     */
    public Lobby getLobby(Player player) {
        Lobby lobby;
        if(player.hasPermission("toastr.lobby.restricted")) {
            lobby = balancer.getLobby(restrictedLobbies);

            if(lobby != null)
                return lobby;
        }

        return balancer.getLobby(lobbies);
    }

    private Balancer getBalancer(String type) {
        switch(type.toUpperCase()) {
            case "LOWEST":
                return new LowestBalancer();
            case "FIRSTAVAILABLE":
                return new FirstAvailableBalancer();
            case "RANDOM":
                return new RandomBalancer();
            case "SEQUENTIAL":
                return new SequentialBalancer();
            default:
                instance.getLogger().error("error trying to get balancer " + type + " is unknown, changing to LOWEST");
                return new LowestBalancer();
        }
    }

}
