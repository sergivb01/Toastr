package services.vortex.toastr.backend.packets.types;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import services.vortex.toastr.ToastrPlugin;
import services.vortex.toastr.backend.packets.Packet;

@NoArgsConstructor
@Getter
@ToString
public class AlertPacket implements Packet {
    private String origin;
    private String message;

    public AlertPacket(String message) {
        this.message = message;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("message", message);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        message = data.get("message").getAsString();
    }
}