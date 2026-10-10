package com.resonanceplanner;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Integration test: needs the test database of Docker Compose (docker compose up -d)
@SpringBootTest
@ActiveProfiles("test")
class ResonancePlannerApplicationTests {

    @Test
    void contextLoads() {
    }

}
