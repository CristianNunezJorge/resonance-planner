package com.resonanceplanner.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Loads catalog and demo events from the JSON files every time the application starts (D39). */
@Component
public class StartupSeeder implements ApplicationRunner {

    private final SeedDataReader seedDataReader;
    private final CatalogSeeder catalogSeeder;
    private final DemoEventSeeder demoEventSeeder;

    public StartupSeeder(SeedDataReader seedDataReader, CatalogSeeder catalogSeeder, DemoEventSeeder demoEventSeeder) {
        this.seedDataReader = seedDataReader;
        this.catalogSeeder = catalogSeeder;
        this.demoEventSeeder = demoEventSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        SeedData data = seedDataReader.read();
        catalogSeeder.seed(data);
        demoEventSeeder.seed(data.events());
    }
}
