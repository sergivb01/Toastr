package dev.sergivos.toastr.backend.packets.types;

import com.google.gson.JsonObject;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class KickPacket implements Packet {
    private String origin;
    private String username;
    private String reason;

    public KickPacket(String username, String reason) {
        this.username = username;
        this.reason = reason;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("username", username);
        data.addProperty("reason", reason);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        username = data.get("username").getAsString();
        reason = data.get("reason").getAsString();
    }
}
