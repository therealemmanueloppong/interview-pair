package dev.emmanuel.interviewpair.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            // 72 is the most bytes bcrypt reads from a password
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String displayName) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserView(UUID id, String email, String displayName) {}

    public record AuthResponse(String token, UserView user) {}
}
