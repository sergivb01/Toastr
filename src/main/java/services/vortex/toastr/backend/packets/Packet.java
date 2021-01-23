package services.vortex.toastr.backend.packets;

import com.google.gson.JsonObject;

public interface Packet {

    int id();

    JsonObject serialize();

    void deserialize(JsonObject object);

}
