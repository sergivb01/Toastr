package dev.sergivos.toastr.backend.packets.types;

import com.google.gson.JsonObject;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.backend.packets.Packet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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