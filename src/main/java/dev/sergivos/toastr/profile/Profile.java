package dev.sergivos.toastr.profile;

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

    public Profile(final Player player) {
        this.uniqueId = player.getUniqueId();
        this.username = player.getUsername();

        this.accountType = AccountType.fromBool(player.isOnlineMode());

        this.firstIP = player.getRemoteAddress().getAddress().getHostAddress();
        this.lastIP = player.getRemoteAddress().getAddress().getHostAddress();

        this.firstLogin = Timestamp.from(Instant.now());
        this.lastLogin = Timestamp.from(Instant.now());
    }

    public enum AccountType {
        CRACKED,
        PREMIUM;

        public static AccountType fromBool(boolean isPremium) {
            return isPremium ? PREMIUM : CRACKED;
        }
    }

    public enum CheckAccountResult {
        DIFFERENT_NAMECASE,
        OLD_PREMIUM,
        ALLOWED
    }

}
