package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class AlertPacket implements Packet {
    private String origin;
    private String message;

    public AlertPacket(String message) {
        this.message = message;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}