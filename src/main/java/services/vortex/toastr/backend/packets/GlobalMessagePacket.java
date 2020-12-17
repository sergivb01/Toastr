package services.vortex.toastr.backend.packets;

import com.google.gson.JsonObject;
import com.minexd.pidgin.packet.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import services.vortex.toastr.ToastrPlugin;

@NoArgsConstructor
@Getter
public class GlobalMessagePacket implements Packet {
    private String origin;
    private String sender;
    private String receiver;
    private String message;

    public GlobalMessagePacket(String sender, String receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public int id() {
        return 4;
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("sender", sender);
        data.addProperty("receiver", receiver);
        data.addProperty("message", message);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        sender = data.get("sender").getAsString();
        receiver = data.get("receiver").getAsString();
        message = data.get("message").getAsString();
    }
}