package services.vortex.toastr.profile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@ToString
@RequiredArgsConstructor
@Getter
public class PlayerData {

    private final UUID uuid;
    private final String username;

    private final long lastOnline;
    private final String ip;

    private final String proxy;
    private final String server;

}
