package dev.sergivos.toastr.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.profile.PlayerData;
import dev.sergivos.toastr.utils.StringUtils;

import java.util.Arrays;
import java.util.Set;

public class PluginMessageListener {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    private static final LegacyChannelIdentifier LEGACY_CHANNEL = new LegacyChannelIdentifier("RedisBungee");
    private static final MinecraftChannelIdentifier MODERN_CHANNEL = MinecraftChannelIdentifier.create("redisbungee", "main");

    public PluginMessageListener() {
        instance.getProxy().getChannelRegistrar().register(LEGACY_CHANNEL, MODERN_CHANNEL);
    }

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
                    players = instance.getRedisManager().getOnlinePlayers().get();
                else
                    players = instance.getRedisManager().getServerCount(server);

                output.writeUTF("PlayerCount");
                output.writeUTF(server);
                output.writeInt(players);
                break;
            }

            case "LASTONLINE": {
                String name = input.readUTF();
                PlayerData data = instance.getCacheManager().getPlayerData(name).orElse(null);

                output.writeUTF("LastOnline");
                output.writeUTF(name);
                output.writeLong(data == null ? 0 : data.getLastOnline());
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
                    final PlayerData playerData = instance.getCacheManager().getPlayerData(username).orElse(null);
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
                String server = input.readUTF();

                Set<String> players;
                if(server.equalsIgnoreCase("ALL")) {
                    players = instance.getRedisManager().getUsernamesOnline();
                } else {
                    players = instance.getRedisManager().getServerUsernames(server);
                }

                output.writeUTF("PlayerList");
                output.writeUTF(server);
                output.writeUTF(StringUtils.joinArray(players));
                break;
            }

            default:
                return;
        }

        connection.sendPluginMessage(event.getIdentifier(), output.toByteArray());
    }

}
