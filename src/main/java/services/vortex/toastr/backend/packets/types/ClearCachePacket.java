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
public class ClearCachePacket implements Packet {
    private String origin;
    private String player;

    public ClearCachePacket(String player) {
        this.player = player;
        this.origin = this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public int id() {
        return 2;
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("player", player);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        player = data.get("player").getAsString();
    }

}