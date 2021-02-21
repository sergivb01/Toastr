package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ClearCachePacket implements Packet {
    private String origin;
    private String player;

    public ClearCachePacket(String player) {
        this.player = player;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}