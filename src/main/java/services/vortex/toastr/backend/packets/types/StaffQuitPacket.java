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
public class StaffQuitPacket implements Packet {
    private String origin;
    private String player;
    private String server;

    public StaffQuitPacket(String player, String server) {
        this.player = player;
        this.server = server;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("player", player);
        data.addProperty("server", server);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        player = data.get("player").getAsString();
        server = data.get("server").getAsString();
    }

}
