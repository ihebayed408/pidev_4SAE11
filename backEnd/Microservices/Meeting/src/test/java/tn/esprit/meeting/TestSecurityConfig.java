package tn.esprit.meeting;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;

/**
 * Test-only security beans. Provides a mock {@link JwtDecoder} so that the
 * OAuth2 resource-server configuration in {@code SecurityConfig} can start
 * without a real Keycloak issuer URI.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }
}
