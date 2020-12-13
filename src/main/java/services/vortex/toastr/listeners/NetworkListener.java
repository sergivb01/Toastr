package services.vortex.toastr.listeners;

import com.velocitypowered.api.event.Subscribe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.redis.RedisManager;
import services.vortex.toastr.utils.PubSubEvent;

public class NetworkListener {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Subscribe
    public void onPubSub(PubSubEvent event) {
        switch(event.getChannel()) {
            case RedisManager.CHANNEL_ALERT:
                final Component alert = instance.getConfig().getMessage("alert")
                        .append(LegacyComponentSerializer.legacyAmpersand().deserialize(event.getMessage()));
                instance.getProxy().getAllPlayers().forEach(player -> player.sendMessage(alert));
                instance.getProxy().getConsoleCommandSource().sendMessage(alert);
                break;

            case RedisManager.CHANNEL_SENDTOALL:
                instance.getProxy().getCommandManager().executeImmediatelyAsync(instance.getProxy().getConsoleCommandSource(), event.getMessage());
                instance.getLogger().info("Running \"" + event.getMessage() + "\" from sendtoall command");
                break;

            default:
                instance.getLogger().warn("Received unhandled pubsub message in channel \"" + event.getChannel() + "\": " + event.getMessage());
                break;
        }
    }

}
