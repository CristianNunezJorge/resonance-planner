package com.resonanceplanner.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One clock for the whole application, so the code that depends on time can be tested with a fixed one. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** The time zone of "today" (D41): a wrong name stops the application at startup. */
    @Bean
    public ZoneId appZone(@Value("${app.time-zone}") String timeZone) {
        return ZoneId.of(timeZone);
    }
}
