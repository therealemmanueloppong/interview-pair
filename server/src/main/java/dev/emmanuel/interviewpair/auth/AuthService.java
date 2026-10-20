package dev.emmanuel.interviewpair.auth;

import dev.emmanuel.interviewpair.auth.AuthDtos.AuthResponse;
import dev.emmanuel.interviewpair.auth.AuthDtos.LoginRequest;
import dev.emmanuel.interviewpair.auth.AuthDtos.RegisterRequest;
import dev.emmanuel.interviewpair.auth.AuthDtos.UserView;
import dev.emmanuel.interviewpair.user.User;
import dev.emmanuel.interviewpair.user.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final Clock clock;
    // Compared against when the email is unknown, so a missing account takes as long as a wrong password.
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.clock = clock;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailTakenException();
        }
        User user = new User(
                UUID.randomUUID(),
                email,
                encoder.encode(request.password()),
                request.displayName().trim(),
                LocalDateTime.now(clock));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailTakenException();
        }
        return respond(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalizeEmail(request.email())).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean matches = encoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }
        return respond(user);
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(tokens.issue(user), view(user));
    }

    public static UserView view(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getDisplayName());
    }

    public static class EmailTakenException extends RuntimeException {
        public EmailTakenException() {
            super("An account with this email already exists");
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid email or password");
        }
    }
}
