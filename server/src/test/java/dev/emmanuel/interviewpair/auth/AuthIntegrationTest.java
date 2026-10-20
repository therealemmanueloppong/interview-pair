package dev.emmanuel.interviewpair.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired PasswordEncoder passwordEncoder;

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private ResultActions register(String email, String password, String name) throws Exception {
        String body = """
                {"email":"%s","password":"%s","displayName":"%s"}""".formatted(email, password, name);
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}""".formatted(email, password);
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String tokenFrom(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    private ResultActions me(String token) throws Exception {
        return mvc.perform(get("/api/me").header("Authorization", "Bearer " + token));
    }

    private String tokenWith(JwtEncoder enc, String subject, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("interviewpair").subject(subject).issuedAt(issuedAt).expiresAt(expiresAt).build();
        return enc.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void registerCreatesAnAccountAndReturnsAToken() throws Exception {
        String email = uniqueEmail();
        register(email, "correct horse", "Ada")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.displayName").value("Ada"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void emailsAreCaseInsensitive() throws Exception {
        String email = uniqueEmail();
        register(email, "correct horse", "Ada").andExpect(status().isCreated());
        register(email.toUpperCase(), "another pass", "Ada 2").andExpect(status().isConflict());
        login(email.toUpperCase(), "correct horse").andExpect(status().isOk());
    }

    @Test
    void emailsWithSpacesAroundThemAreRejectedAsInvalid() throws Exception {
        register("  " + uniqueEmail() + " ", "correct horse", "Ada")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void duplicateEmailIsRejectedWith409() throws Exception {
        String email = uniqueEmail();
        register(email, "correct horse", "Ada").andExpect(status().isCreated());
        register(email, "different pass", "Imposter")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("An account with this email already exists"));
    }

    @Test
    void invalidRegistrationsAreRejectedWithFieldErrors() throws Exception {
        register("not-an-email", "correct horse", "Ada")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
        register(uniqueEmail(), "short", "Ada")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
        register(uniqueEmail(), "correct horse", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.displayName").exists());
    }

    @Test
    void passwordsAreStoredHashedNotInPlainText() {
        String hash = passwordEncoder.encode("correct horse");
        assertThat(hash).isNotEqualTo("correct horse").startsWith("$2");
        assertThat(passwordEncoder.matches("correct horse", hash)).isTrue();
    }

    @Test
    void loginReturnsATokenThatOpensTheProfileEndpoint() throws Exception {
        String email = uniqueEmail();
        register(email, "correct horse", "Ada").andExpect(status().isCreated());
        String token = tokenFrom(login(email, "correct horse").andExpect(status().isOk()));
        me(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Ada"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        String email = uniqueEmail();
        register(email, "correct horse", "Ada").andExpect(status().isCreated());
        String wrongPassword = login(email, "wrong password").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login(uniqueEmail(), "correct horse").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    @Test
    void profileEndpointNeedsAToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void garbageAndTamperedTokensAreRejected() throws Exception {
        String email = uniqueEmail();
        String token = tokenFrom(register(email, "correct horse", "Ada"));
        me("not-a-token").andExpect(status().isUnauthorized());
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"%s\",\"iss\":\"interviewpair\",\"exp\":9999999999}"
                        .formatted(UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
        me(parts[0] + "." + forgedPayload + "." + parts[2]).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokensAreRejected() throws Exception {
        String email = uniqueEmail();
        String id = JsonPath.read(
                register(email, "correct horse", "Ada").andReturn().getResponse().getContentAsString(),
                "$.user.id");
        Instant longAgo = Instant.now().minusSeconds(7200);
        me(tokenWith(encoder, id, longAgo, longAgo.plusSeconds(60))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokensSignedWithAnotherSecretAreRejected() throws Exception {
        String id = JsonPath.read(
                register(uniqueEmail(), "correct horse", "Ada").andReturn().getResponse().getContentAsString(),
                "$.user.id");
        var otherKey = new SecretKeySpec(
                "a-completely-different-secret-key-0000".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder other = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));
        Instant now = Instant.now();
        me(tokenWith(other, id, now, now.plusSeconds(3600))).andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenForADeletedAccountIsTreatedAsLoggedOut() throws Exception {
        Instant now = Instant.now();
        String ghost = tokenWith(encoder, UUID.randomUUID().toString(), now, now.plusSeconds(3600));
        me(ghost).andExpect(status().isUnauthorized());
    }
}
