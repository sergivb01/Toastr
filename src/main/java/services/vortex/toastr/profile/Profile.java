package services.vortex.toastr.profile;

import com.velocitypowered.api.proxy.Player;
import lombok.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class Profile {
    @Getter
    private static final ConcurrentHashMap<UUID, Profile> profiles = new ConcurrentHashMap<>();

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
        PREMIUM;

        public AccountType fromBool(boolean isPremium) {
            return isPremium ? PREMIUM : CRACKED;
        }
    }

    public enum CheckAccountResult {
        DIFFERENT_NAMECASE,
        OLD_PREMIUM,
        ALLOWED
    }

}
