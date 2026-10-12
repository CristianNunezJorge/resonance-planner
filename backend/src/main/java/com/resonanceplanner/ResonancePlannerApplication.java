package com.resonanceplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Excluded because the users are in the database and the login is ours (T12): without this, Spring Boot would
// create an extra user with a password generated at every start
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ResonancePlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResonancePlannerApplication.class, args);
    }

}
