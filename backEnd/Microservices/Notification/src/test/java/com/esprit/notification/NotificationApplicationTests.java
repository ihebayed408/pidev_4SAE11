package com.esprit.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:http://127.0.0.1:65534",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:65535/mock-jwks",
    "notification.firebase.enabled=false"
})
@SpringBootTest
class NotificationApplicationTests {

    @Test
    void contextLoads() {
    }
}
