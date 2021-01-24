package services.vortex.toastr.backend;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BackendCredentials {
    private final String hostname;
    private final int port;
    private final String username;
    private final String password;
    private final String database;
}
