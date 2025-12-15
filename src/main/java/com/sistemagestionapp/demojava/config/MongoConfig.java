package com.sistemagestionapp.demojava.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mongo")
public class MongoConfig {

    @Value("${spring.data.mongodb.host:mongo}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port:27017}")
    private int mongoPort;

    @Value("${spring.data.mongodb.database:demo}")
    private String mongoDatabase;

    @Value("${spring.data.mongodb.username:demo}")
    private String mongoUsername;

    @Value("${spring.data.mongodb.password:demo}")
    private String mongoPassword;

    @Value("${spring.data.mongodb.authentication-database:admin}")
    private String authDatabase;

    @Bean
    public MongoClient mongoClient() {
        String uri = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                mongoUsername,
                mongoPassword,
                mongoHost,
                mongoPort,
                mongoDatabase,
                authDatabase
        );
        return MongoClients.create(uri);
    }
}