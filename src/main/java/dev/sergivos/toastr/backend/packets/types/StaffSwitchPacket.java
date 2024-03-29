package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class StaffSwitchPacket implements Packet {
    private String origin;
    private String player;
    private String from;
    private String to;

    public StaffSwitchPacket(String player, String from, String to) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}
