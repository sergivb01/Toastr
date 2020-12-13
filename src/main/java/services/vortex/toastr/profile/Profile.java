package services.vortex.toastr.profile;

import com.velocitypowered.api.proxy.Player;
import lombok.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class Profile {
    @Getter
    private static final HashMap<UUID, Profile> profiles = new HashMap<>();

    private final UUID uniqueId;
    private final String username;
    private AccountType accountType;
    private String firstIP;
    private String lastIP;

    private Timestamp firstLogin;
    private Timestamp lastLogin;

    private String password;
    private String salt;

    private boolean loggedIn = false;

    public static Profile createProfile(Player player) {
        Profile profile = new Profile(player.getUniqueId(), player.getUsername());

        profile.accountType = player.isOnlineMode() ? AccountType.PREMIUM : AccountType.CRACKED;
        profile.firstIP = player.getRemoteAddress().getAddress().getHostAddress();
        profile.lastIP = player.getRemoteAddress().getAddress().getHostAddress();
        profile.firstLogin = Timestamp.from(Instant.now());

        return profile;
    }

    public enum AccountType {
        CRACKED,
        PREMIUM,
    }

}
