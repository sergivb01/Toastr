package services.vortex.toastr.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.profile.PlayerData;

import java.util.Arrays;

public class PluginMessageListener {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    private static final LegacyChannelIdentifier LEGACY_CHANNEL = new LegacyChannelIdentifier("RedisBungee");
    private static final MinecraftChannelIdentifier MODERN_CHANNEL = MinecraftChannelIdentifier.create("redisbungee", "main");

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if(!event.getIdentifier().equals(LEGACY_CHANNEL) && !event.getIdentifier().equals(MODERN_CHANNEL))
            return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if(!(event.getSource() instanceof ServerConnection))
            return;

        ServerConnection connection = (ServerConnection) event.getSource();
        ByteArrayDataInput input = event.dataAsDataStream();
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        String subChannel = input.readUTF();
        switch(subChannel.toUpperCase()) {
            case "PLAYERCOUNT": {
                String server = input.readUTF();
                int players;
                if(server.equalsIgnoreCase("ALL"))
                    players = instance.getRedisManager().getOnlinePlayers();
                else
                    players = Math.toIntExact(instance.getRedisManager().getServerCount(server));

                output.writeUTF("PlayerCount");
                output.writeUTF(server);
                output.writeInt(players);
                break;
            }

            case "LASTONLINE": {
                String name = input.readUTF();
                PlayerData data = instance.getCacheManager().getPlayerData(name);

                output.writeUTF("LastOnline");
                output.writeUTF(name);
                output.writeLong(data.getLastOnline());
                break;
            }

            case "PROXY": {
                output.writeUTF("Proxy");
                output.writeUTF(instance.getRedisManager().getProxyName());
                break;
            }

            case "PLAYERPROXY": {
                String username = input.readUTF();

                output.writeUTF("PlayerProxy");
                output.writeUTF(username);

                if(instance.getProxy().getPlayer(username).isPresent())
                    output.writeUTF(instance.getRedisManager().getProxyName());
                else {
                    final PlayerData playerData = instance.getCacheManager().getPlayerData(username);
                    output.writeUTF(playerData == null ? "unknown" : playerData.getProxy());
                }
                break;
            }


            case "SERVERPLAYERS": {
                instance.getLogger().error(event.getSource().toString() + " tried to send a \"ServerPlayers\" plugin message, but feature is not implemented yet!");
                instance.getLogger().error("Data: " + Arrays.toString(event.getData()));
                break;
            }

            case "PLAYERLIST": {
//                String server = input.readUTF();
//                Set<UUID> players = instance.getRedisManager().getServerUUIDs(server);
//                StringBuilder sb = new StringBuilder();
//                for(UUID uuid : players)
//                    sb.append(uuid).append(", ");
//
//                if(sb.length() != 0)
//                    sb.setLength(sb.length() - 2);
//
//                output.writeUTF("PlayerList");
//                output.writeUTF(server);
//                output.writeUTF(sb.toString());
                instance.getLogger().error(event.getSource().toString() + " tried to send a \"ServerPlayers\" plugin message, but feature is not implemented yet!");
                instance.getLogger().error("Data: " + Arrays.toString(event.getData()));
                break;
            }

            default:
                return;
        }

        connection.sendPluginMessage(event.getIdentifier(), output.toByteArray());
    }

}
