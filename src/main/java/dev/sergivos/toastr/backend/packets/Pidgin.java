package dev.sergivos.toastr.backend.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.handler.IncomingPacketHandler;
import dev.sergivos.toastr.backend.packets.listener.PacketListener;
import dev.sergivos.toastr.backend.packets.listener.PacketListenerData;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public class Pidgin {
    private static final Logger logger = ToastrPlugin.getInstance().getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final String channel;
    private final JedisPool pool;

    private final List<PacketListenerData> packetListeners;

    private final Map<String, Class<? extends Packet>> idToType = new HashMap<>();
    private final Map<Class<? extends Packet>, String> typeToId = new HashMap<>();

    private JedisPubSub jedisPubSub;

    public Pidgin(String channel, final JedisPool pool) {
        this.channel = channel;
        this.pool = pool;

        this.packetListeners = new ArrayList<>();

        this.setupPubSub();
    }

    public void sendPacket(Packet packet) {
        try(Jedis jedis = this.pool.getResource()) {
            final String object = GSON.toJson(packet);
            if(object == null) {
                throw new IllegalStateException("Packet cannot generate null serialized data");
            }

            final String id = typeToId.get(packet.getClass());
            if(id == null) {
                throw new IllegalStateException("Packet does not have an id");
            }

            jedis.publish(this.channel, id + ";" + object);
        } catch(Exception ex) {
            logger.error("[Pidgin] Failed to publish packet...", ex);
        }
    }

    public void registerPacket(Class<? extends Packet> clazz) {
        try {
            final String id = clazz.getSimpleName();
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
                Class<? extends Packet> packetClass = null;

                if(method.getParameters().length > 0) {
                    final Class<?> type = method.getParameters()[0].getType();
                    if(Packet.class.isAssignableFrom(type)) {
                        packetClass = (Class<? extends Packet>) type;
                    }
                }

                if(packetClass != null) {
                    this.packetListeners.add(new PacketListenerData(packetListener, method, packetClass));
                }
            }
        }
    }

    private void setupPubSub() {
        logger.debug("[Pidgin] Setting up PubSup..");

        this.jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if(!channel.equalsIgnoreCase(Pidgin.this.channel)) return;

                try {
                    String[] args = message.split(";");
                    String id = args[0];
                    if(!idToType.containsKey(id)) {
                        throw new IllegalStateException("A packet with that ID does not exist");
                    }

                    Packet packet = GSON.fromJson(args[1], idToType.get(id));
                    if(packet != null) {
                        for(PacketListenerData data : packetListeners) {
                            if(data.matches(packet)) {
                                data.getMethod().invoke(data.getInstance(), packet);
                            }
                        }
                    }
                } catch(Exception e) {
                    logger.error("[Pidgin] Failed to handle message", e);
                    e.printStackTrace();
                }
            }
        };

        ForkJoinPool.commonPool().execute(() -> {
            try(final Jedis jedis = this.pool.getResource()) {
                jedis.subscribe(this.jedisPubSub, channel);
                logger.debug("[Pidgin] Successfully subscribing to channel..");
            } catch(Exception ex) {
                logger.error("[Pidgin] Failed to subscribe to channel..", ex);
            }
        });
    }

}