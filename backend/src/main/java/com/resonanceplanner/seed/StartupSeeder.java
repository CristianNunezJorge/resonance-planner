package com.resonanceplanner.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Loads catalog and demo events from the JSON files and creates the admin account every time the application starts (D19, D39). */
@Component
public class StartupSeeder implements ApplicationRunner {

    private final SeedDataReader seedDataReader;
    private final CatalogSeeder catalogSeeder;
    private final DemoEventSeeder demoEventSeeder;
    private final AdminSeeder adminSeeder;

    public StartupSeeder(SeedDataReader seedDataReader, CatalogSeeder catalogSeeder, DemoEventSeeder demoEventSeeder,
            AdminSeeder adminSeeder) {
        this.seedDataReader = seedDataReader;
        this.catalogSeeder = catalogSeeder;
        this.demoEventSeeder = demoEventSeeder;
        this.adminSeeder = adminSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        SeedData data = seedDataReader.read();
        catalogSeeder.seed(data);
        demoEventSeeder.seed(data.events());
        adminSeeder.seed();
    }
}
