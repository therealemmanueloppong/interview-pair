package dev.emmanuel.interviewpair.api;

import dev.emmanuel.interviewpair.auth.AuthDtos.UserView;
import dev.emmanuel.interviewpair.auth.AuthService;
import dev.emmanuel.interviewpair.user.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/me")
    public UserView me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(UUID.fromString(jwt.getSubject()))
                .map(AuthService::view)
                // A valid token for an account that no longer exists is treated as logged out.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
