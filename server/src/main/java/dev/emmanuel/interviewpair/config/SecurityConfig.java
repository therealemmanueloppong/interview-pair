package dev.emmanuel.interviewpair.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Clients log in at /api/auth/login, then send "Authorization: Bearer <token>" on later calls.
    // Anything outside /api and /actuator is the React app's static files and is public. The /ws
    // handshake is open to HTTP security because browsers cannot send the Bearer header there. The
    // socket authenticates with a one-time ticket instead (see WsTicketService).
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/health", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/**", "/actuator/**").authenticated()
                .anyRequest().permitAll());
        return http.build();
    }
}
