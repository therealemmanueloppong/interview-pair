package dev.emmanuel.interviewpair.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings for signing login tokens. The secret comes from the JWT_SECRET environment variable. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, @DefaultValue("PT1H") Duration ttl) {

    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to at least " + MIN_SECRET_BYTES + " characters");
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("app.jwt.ttl must be positive");
        }
    }
}
