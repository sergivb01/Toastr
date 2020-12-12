package services.vortex.toastr.profile;

import com.velocitypowered.api.proxy.Player;
import lombok.*;

import java.util.HashMap;
import java.util.UUID;

@Getter
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
    @Setter
    private String lastIP;

    private long firstLogin;
    @Setter
    private long lastLogin;
    @Setter
    private String lastServer;
    @Setter
    private String password;

    @Setter
    private boolean loggedIn = false;

    public static Profile createProfile(Player player) {
        Profile profile = new Profile(player.getUniqueId(), player.getUsername());

        profile.accountType = player.isOnlineMode() ? AccountType.PREMIUM : AccountType.CRACKED;
        profile.firstIP = player.getRemoteAddress().getHostName();
        profile.lastIP = player.getRemoteAddress().getHostName();
        profile.firstLogin = System.currentTimeMillis();
        profile.lastServer = !player.getCurrentServer().isPresent() ? "unknown" : player.getCurrentServer().get().getServerInfo().getName();

        return profile;
    }

    public enum AccountType {
        CRACKED,
        PREMIUM,
    }

}
