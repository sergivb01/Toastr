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
public class NetworkStatusPacket implements Packet {
    private String origin;
    private String proxy;
    private boolean up;

    public NetworkStatusPacket(String proxy, boolean up) {
        this.proxy = proxy;
        this.up = up;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("proxy", proxy);
        data.addProperty("up", up);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        proxy = data.get("proxy").getAsString();
        up = data.get("up").getAsBoolean();
    }
}
