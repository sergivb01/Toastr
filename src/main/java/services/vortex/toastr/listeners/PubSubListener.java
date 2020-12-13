package services.vortex.toastr.listeners;

import redis.clients.jedis.JedisPubSub;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.utils.PubSubEvent;

public class PubSubListener extends JedisPubSub {

    private static final ToastrPlugin instance = ToastrPlugin.getInstance();

    @Override
    public void onMessage(String channel, String message) {
        instance.getProxy().getEventManager().fireAndForget(new PubSubEvent(channel, message));
    }

}
