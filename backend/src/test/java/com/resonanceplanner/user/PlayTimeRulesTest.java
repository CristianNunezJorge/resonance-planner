package com.resonanceplanner.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.testsupport.TestGameRules;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// The options of the daily play time come from game-rules.json: 30, 60 or 120 minutes
class PlayTimeRulesTest {

    private final PlayTimeRules rules = new PlayTimeRules(TestGameRules.withPlayTimeOptions(30, 60, 120));

    @ParameterizedTest
    @ValueSource(ints = {30, 60, 120})
    void acceptsTheOptionsOfTheGameRules(int minutes) {
        assertThat(rules.problem(minutes)).isEmpty();
        assertThatCode(() -> rules.requireValid(minutes)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 45, 90, 121, -30, Integer.MAX_VALUE})
    void refusesAnythingElseAndListsTheOptions(int minutes) {
        assertThat(rules.problem(minutes)).contains("Scegli un tempo di gioco tra 30, 60 o 120 minuti.");
    }

    @Test
    void requireValidNamesTheField() {
        assertThatThrownBy(() -> rules.requireValid(45))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        e -> assertThat(e.getFields()).isEqualTo(Map.of("playTimeMinutes",
                                "Scegli un tempo di gioco tra 30, 60 o 120 minuti.")));
    }

    @Test
    void describesASingleOptionWithoutAnOr() {
        PlayTimeRules oneOption = new PlayTimeRules(TestGameRules.withPlayTimeOptions(60));

        assertThat(oneOption.problem(30)).contains("Scegli un tempo di gioco tra 60 minuti.");
    }
}
