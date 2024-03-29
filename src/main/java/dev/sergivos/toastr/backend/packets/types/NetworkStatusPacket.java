package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class NetworkStatusPacket implements Packet {
    private String origin;
    private String proxy;
    private boolean up;

    public NetworkStatusPacket(String proxy, boolean up) {
        this.proxy = proxy;
        this.up = up;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}
