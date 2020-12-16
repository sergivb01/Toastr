package services.vortex.toastr.backend.packets;

import com.google.gson.JsonObject;
import com.minexd.pidgin.packet.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import services.vortex.toastr.ToastrPlugin;

@NoArgsConstructor
@Getter
public class AlertPacket implements Packet {
    private String origin;
    private String message;

    public AlertPacket(String message) {
        this.message = message;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public int id() {
        return 1;
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