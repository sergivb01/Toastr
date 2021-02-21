package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class GlobalMessagePacket implements Packet {
    private String origin;
    private String sender;
    private String receiver;
    private String message;

    public GlobalMessagePacket(String sender, String receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}