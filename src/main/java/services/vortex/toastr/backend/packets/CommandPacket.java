package services.vortex.toastr.backend.packets;

import com.google.gson.JsonObject;
import com.minexd.pidgin.packet.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import services.vortex.toastr.ToastrPlugin;

@NoArgsConstructor
@Getter
@ToString
public class CommandPacket implements Packet {
    private String origin;
    private String command;

    public CommandPacket(String command) {
        this.command = command;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public int id() {
        return 3;
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("command", command);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        command = data.get("command").getAsString();
    }
}