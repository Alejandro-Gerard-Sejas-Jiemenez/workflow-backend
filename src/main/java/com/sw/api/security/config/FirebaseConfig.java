package com.sw.api.security.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            ClassPathResource serviceAccount = new ClassPathResource("service-account.json");
            
            if (!serviceAccount.exists()) {
                System.out.println("⚠️ [FIREBASE] service-account.json no encontrado en resources. Las notificaciones Push NO funcionaran.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("🔥 [FIREBASE] Inicializado con éxito.");
            }
        } catch (IOException e) {
            System.err.println("❌ [FIREBASE] Error al inicializar: " + e.getMessage());
        }
    }
}

