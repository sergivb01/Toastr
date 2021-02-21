package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CommandPacket implements Packet {
    private String origin;
    private String command;

    public CommandPacket(String command) {
        this.command = command;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}