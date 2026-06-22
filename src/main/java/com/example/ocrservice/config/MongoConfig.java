package com.example.ocrservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit MongoClient so the connection string is ALWAYS honored.
 *
 * Background: on Spring Boot 4.x the plain `spring.data.mongodb.host/port`
 * (and even `uri`) auto-config was silently falling back to the driver default
 * (localhost:27017) inside Docker, which caused "Connection refused" because
 * inside the container localhost is the app itself, not the mongo container.
 *
 * Defining the MongoClient bean here removes any ambiguity: we read the
 * connection string from configuration and build the client directly.
 *
 * The value resolves in this order:
 *   1. SPRING_DATA_MONGODB_URI env var (set by docker-compose to
 *      mongodb://mongodb:27017/ocr_db)
 *   2. the default below, for running the jar directly on your machine.
 */
@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/ocr_db}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(settings);
    }
}
