package services.vortex.toastr.backend.packets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.handler.IncomingPacketHandler;
import services.vortex.toastr.backend.packets.listener.PacketListener;
import services.vortex.toastr.backend.packets.listener.PacketListenerData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public class Pidgin {
    private static final Logger logger = ToastrPlugin.getInstance().getLogger();
    private final String channel;
    private final JedisPool pool;
    private final List<PacketListenerData> packetListeners;
    private final Map<Integer, Class> idToType = new HashMap<>();
    private final Map<Class, Integer> typeToId = new HashMap<>();
    private JedisPubSub jedisPubSub;

    public Pidgin(String channel, final JedisPool pool) {
        this.channel = channel;
        this.pool = pool;

        this.packetListeners = new ArrayList<>();

        this.setupPubSub();
    }

    public void sendPacket(Packet packet) {
        try(Jedis jedis = this.pool.getResource()) {
            final JsonObject object = packet.serialize();
            if(object == null) {
                throw new IllegalStateException("Packet cannot generate null serialized data");
            }

            jedis.publish(this.channel, packet.id() + ";" + object.toString());
        } catch(Exception ex) {
            logger.error("[Pidgin] Failed to publish packet...", ex);
        }
    }

    public Packet buildPacket(int id) {
        if(!idToType.containsKey(id)) {
            throw new IllegalStateException("A packet with that ID does not exist");
        }

        try {
            return (Packet) idToType.get(id).newInstance();
        } catch(Exception e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Could not create new instance of packet type");
    }

    public void registerPacket(Class clazz) {
        try {
            int id = (int) clazz.getDeclaredMethod("id").invoke(clazz.newInstance(), null);

            if(idToType.containsKey(id) || typeToId.containsKey(clazz)) {
                throw new IllegalStateException("A packet with that ID has already been registered");
            }

            idToType.put(id, clazz);
            typeToId.put(clazz, id);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void registerListener(PacketListener packetListener) {
        for(Method method : packetListener.getClass().getDeclaredMethods()) {
            if(method.getDeclaredAnnotation(IncomingPacketHandler.class) != null) {
                Class packetClass = null;

                if(method.getParameters().length > 0) {
                    if(Packet.class.isAssignableFrom(method.getParameters()[0].getType())) {
                        packetClass = method.getParameters()[0].getType();
                    }
                }

                if(packetClass != null) {
                    this.packetListeners.add(new PacketListenerData(packetListener, method, packetClass));
                }
            }
        }
    }

    private void setupPubSub() {
        logger.info("[Pidgin] Setting up PubSup..");

        this.jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if(!channel.equalsIgnoreCase(Pidgin.this.channel)) return;

                try {
                    String[] args = message.split(";");
                    Integer id = Integer.valueOf(args[0]);
                    Packet packet = buildPacket(id);

                    if(packet != null) {
                        packet.deserialize(JsonParser.parseString(args[1]).getAsJsonObject());
                        for(PacketListenerData data : packetListeners) {
                            if(data.matches(packet)) {
                                data.getMethod().invoke(data.getInstance(), packet);
                            }
                        }
                    }
                } catch(Exception e) {
                    logger.info("[Pidgin] Failed to handle message");
                    e.printStackTrace();
                }
            }
        };

        ForkJoinPool.commonPool().execute(() -> {
            try(final Jedis jedis = this.pool.getResource()) {
                jedis.subscribe(this.jedisPubSub, channel);
                logger.info("[Pidgin] Successfully subscribing to channel..");
            } catch(Exception ex) {
                logger.error("[Pidgin] Failed to subscribe to channel..", ex);
            }
        });
    }

}