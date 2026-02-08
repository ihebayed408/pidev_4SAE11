package com.esprit.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration with description for the Gestion User microservice.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestion User API")
                        .version("1.0")
                        .description("""
                                **User Management Microservice** for the Smart Freelance and Project Matching Platform.
                                
                                This API allows you to:
                                - List and retrieve users
                                - Create new users (CLIENT, FREELANCER, ADMIN)
                                - Update and delete users
                                - Look up users by ID or email
                                
                                Passwords are hashed with BCrypt before storage and are never returned in responses.
                                **Authentication and authorization are handled separately by Keycloak** in a dedicated microservice.
                                """)
                        .contact(new Contact()
                                .name("Esprit")
                                .email("contact@esprit.tn"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://esprit.tn")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ));
    }
}
