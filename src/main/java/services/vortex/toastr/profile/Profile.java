package services.vortex.toastr.profile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.UUID;

@Getter
@ToString
@RequiredArgsConstructor
public class Profile {
    @Getter
    private static final HashMap<UUID, Profile> profiles = new HashMap<>();

    private final UUID uniqueID;
    private final String username;
    private AccountType accountType;
    private String firstIP;
    private String lastIP;

    private long firstLogged;
    private long lastLoggedIn;
    private String lastLoggedInAt;

    enum AccountType {
        UNREGISTERED,
        CRACKED,
        PREMIUM,
        FORCED_CRACKED,
        FORCED_PREMIUM
    }

}
