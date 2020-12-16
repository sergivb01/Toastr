package services.vortex.toastr.lobbby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import lombok.Getter;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.lobbby.balancers.FirstAvailableBalancer;
import services.vortex.toastr.lobbby.balancers.LowestBalancer;
import services.vortex.toastr.lobbby.balancers.RandomBalancer;
import services.vortex.toastr.lobbby.balancers.SequentialBalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

        final JsonObject config = instance.getConfig().getObject().get("lobby").getAsJsonObject();

        for(JsonElement element : config.getAsJsonArray("servers")) {
            JsonObject object = element.getAsJsonObject();
            String name = object.get("name").getAsString();
            boolean restricted = object.get("restricted").getAsBoolean();

            Optional<RegisteredServer> optionalServer = instance.getProxy().getServer(name);
            if(!optionalServer.isPresent()) {
                instance.getLogger().warn("Invalid server name: " + name);
                continue;
            }

            RegisteredServer server = optionalServer.get();
            Lobby lobby = new Lobby(name, restricted, server);

            server.ping().whenComplete((ping, exception) -> {
                if(ping == null) {
                    instance.getLogger().warn("An error occurred while pinging " + lobby.getName(), exception);
                    return;
                }

                Optional<ServerPing.Players> players = ping.getPlayers();
                if(!players.isPresent())
                    return;

                lobby.setMaxPlayers(players.get().getMax());
            });

            if(restricted) {
                tmp_restricted.add(lobby);
            } else {
                tmp.add(lobby);
            }
        }
        lobbies.clear();
        restrictedLobbies.clear();

        lobbies = tmp;
        restrictedLobbies = tmp_restricted;

        instance.getLogger().info("Loaded " + lobbies.size() + " regular lobbies.");
        instance.getLogger().info("Loaded " + restrictedLobbies.size() + " restricted lobbies.");

        balancer = getBalancer(config.get("load-balancer").getAsString());
        sendPlayerToHubOnClose = config.get("send-on-close").getAsBoolean();
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
