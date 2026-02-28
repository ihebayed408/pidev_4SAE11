package com.esprit.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    @Value("${notification.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream stream = credentialsStream();
            if (stream == null) {
                throw new IllegalStateException(
                    "Firebase credentials not set. Set GOOGLE_APPLICATION_CREDENTIALS env var or notification.firebase.credentials-path");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    private InputStream credentialsStream() throws IOException {
        if (StringUtils.hasText(credentialsPath)) {
            java.io.File f = new java.io.File(credentialsPath.trim());
            if (f.isFile()) return new FileInputStream(f);
        }
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(envPath)) {
            java.io.File f = new java.io.File(envPath.trim());
            if (f.isFile()) return new FileInputStream(f);
        }
        // Fallback: project root (when run from project root, user.dir is root)
        String fallback = System.getProperty("user.dir") + "/notificationsystem-3bd2f-firebase-adminsdk-fbsvc-a00d605692.json";
        java.io.File fallbackFile = new java.io.File(fallback);
        if (fallbackFile.isFile()) return new FileInputStream(fallbackFile);
        return null;
    }
}
