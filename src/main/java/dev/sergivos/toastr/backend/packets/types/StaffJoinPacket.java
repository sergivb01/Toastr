package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class StaffJoinPacket implements Packet {
    private String origin;
    private String player;
    private String server;

    public StaffJoinPacket(String player, String server) {
        this.player = player;
        this.server = server;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}
