package dev.sergivos.toastr.backend.packets.types;

import dev.sergivos.toastr.ToastrPlugin;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class KickPacket {
    private String origin;
    private String username;
    private String reason;

    public KickPacket(String username, String reason) {
        this.username = username;
        this.reason = reason;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

}
