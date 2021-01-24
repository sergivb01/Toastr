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
public class StaffSwitchPacket implements Packet {
    private String origin;
    private String player;
    private String from;
    private String to;

    public StaffSwitchPacket(String player, String from, String to) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.origin = ToastrPlugin.getInstance().getRedisManager().getProxyName();
    }

    @Override
    public JsonObject serialize() {
        JsonObject data = new JsonObject();
        data.addProperty("origin", origin);
        data.addProperty("player", player);
        data.addProperty("from", from);
        data.addProperty("to", to);
        return data;
    }

    @Override
    public void deserialize(JsonObject data) {
        origin = data.get("origin").getAsString();
        player = data.get("player").getAsString();
        from = data.get("from").getAsString();
        to = data.get("to").getAsString();
    }

}
