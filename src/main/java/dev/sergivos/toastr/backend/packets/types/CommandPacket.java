package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CommandPacket {
    private String origin;
    private String command;

    public CommandPacket(String command) {
        this.command = command;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}