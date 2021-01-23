package services.vortex.toastr.backend.packets.types;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.Packet;

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
    public int id() {
        return 6;
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
