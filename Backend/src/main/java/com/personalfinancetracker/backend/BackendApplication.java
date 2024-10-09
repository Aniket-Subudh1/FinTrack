package com.personalfinancetracker.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.load();

        // Optionally, you can access specific environment variables for debugging purposes
        System.out.println("DB Username: " + dotenv.get("DB_USERNAME"));
        System.out.println("Google Client ID: " + dotenv.get("GOOGLE_CLIENT_ID"));

        SpringApplication.run(BackendApplication.class, args);
    }
}
