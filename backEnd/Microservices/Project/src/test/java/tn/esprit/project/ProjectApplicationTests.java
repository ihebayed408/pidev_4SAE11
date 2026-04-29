package tn.esprit.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(classes = {ProjectApplication.class, ProjectApplicationTests.StubSecurityBeans.class})
class ProjectApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class StubSecurityBeans {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("JwtDecoder is stubbed for tests");
            };
        }
    }
}
