package com.resonanceplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ResonancePlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResonancePlannerApplication.class, args);
    }

}
