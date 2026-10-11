package com.resonanceplanner.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.common.data.DataFileReader;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GameRulesConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(DataFileReader.class, GameRulesConfig.class);

    @Test
    void readsTheGameRulesFromTheJsonFile() {
        contextRunner.run(context -> {
            GameRules rules = context.getBean(GameRules.class);

            assertThat(rules.waveplate().max()).isEqualTo(240);
            assertThat(rules.waveplate().regenMinutes()).isEqualTo(6);
            assertThat(rules.waveplateCrystals().max()).isEqualTo(480);
            assertThat(rules.ascensionLevels()).containsExactly(20, 40);
            assertThat(rules.maxLevel()).isEqualTo(40);
            assertThat(rules.shellCreditPerExp()).isEqualByComparingTo("0.35");
            assertThat(rules.demoEventsAnchorDate()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(rules.playTimeOptionsMinutes()).containsExactly(30, 60, 120);
        });
    }
}
