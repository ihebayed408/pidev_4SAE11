package com.esprit.notification;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
@SpringBootTest
class NotificationApplicationTests {

    @MockitoBean
    private Firestore firestore;

    @Test
    void contextLoads() {
    }
}
