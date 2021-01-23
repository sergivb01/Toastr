package services.vortex.toastr.backend.packets.listener;

import services.vortex.toastr.backend.packets.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Method;

/**
 * A wrapper class that holds all the information needed to
 * identify and execute a message function.
 */
@AllArgsConstructor
@Getter
public class PacketListenerData {

    private final Object instance;
    private final Method method;
    private final Class packetClass;

    public boolean matches(Packet packet) {
        return this.packetClass == packet.getClass();
    }

}
