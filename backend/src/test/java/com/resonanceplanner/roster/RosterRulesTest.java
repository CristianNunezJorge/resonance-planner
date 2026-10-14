package com.resonanceplanner.roster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.catalog.GameRules;
import com.resonanceplanner.catalog.GameRules.Regeneration;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.testsupport.TestGameRules;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Level and ascension rank of a character (D34, D104): rank 0 = levels 1-20, rank 1 = 20-40, rank 2 = only 40
class RosterRulesTest {

    private final RosterRules rules = new RosterRules(TestGameRules.standard());

    private Map<String, String> problemsOf(int currentLevel, int currentRank, int targetLevel, int targetRank) {
        return rules.problems(new Progress(currentLevel, currentRank, targetLevel, targetRank));
    }

    @ParameterizedTest
    @CsvSource({
        // from nothing to the cap
        "1,0,40,2",
        // the same level before and after the ascension is the same level of two different ranks
        "20,0,20,1",
        "20,0,20,0",
        "20,1,40,1",
        "20,1,20,1",
        "40,2,40,2",
        "5,0,20,1",
        "1,0,1,0",
        "40,1,40,2",
    })
    void acceptsTheCombinationsThatExistInTheGame(int currentLevel, int currentRank, int targetLevel, int targetRank) {
        assertThat(problemsOf(currentLevel, currentRank, targetLevel, targetRank)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        // a level above the maximum of its rank
        "21,0,40,2,currentLevel",
        "1,0,41,2,targetLevel",
        "1,0,25,0,targetLevel",
        // a level below the minimum of its rank: level 5 cannot be after an ascension
        "5,1,40,1,currentLevel",
        "1,0,10,1,targetLevel",
        // rank 2 is only level 40
        "39,2,40,2,currentLevel",
        "1,0,39,2,targetLevel",
        // level below 1
        "0,0,20,0,currentLevel",
        "-3,0,20,0,currentLevel",
        "1,0,0,0,targetLevel",
        // a rank that does not exist: the level of that side is not judged too
        "1,3,40,2,currentRank",
        "1,-1,40,2,currentRank",
        "1,0,40,3,targetRank",
        "1,0,40,-1,targetRank",
        "50,5,40,2,currentRank",
        // the goal cannot be lower than the present: first the rank, then the level
        "30,1,25,1,targetLevel",
        "40,2,20,1,targetRank",
        "30,1,20,0,targetRank",
    })
    void refusesTheOthersAndNamesTheWrongField(int currentLevel, int currentRank, int targetLevel, int targetRank,
            String field) {
        assertThat(problemsOf(currentLevel, currentRank, targetLevel, targetRank)).containsOnlyKeys(field);
    }

    @Test
    void reportsAllTheWrongFieldsAtOnce() {
        assertThat(problemsOf(21, 0, 41, 2)).containsOnlyKeys("currentLevel", "targetLevel");
        assertThat(problemsOf(5, 1, 10, 1)).containsOnlyKeys("currentLevel", "targetLevel");
    }

    @Test
    void doesNotCompareTheGoalWithAPresentThatIsNotValid() {
        // The present is wrong by itself: saying also that the goal is lower would only be noise
        assertThat(problemsOf(99, 0, 20, 0)).containsOnlyKeys("currentLevel");
    }

    @Test
    void explainsTheRightRangeInItalian() {
        assertThat(problemsOf(21, 0, 40, 2)).containsEntry("currentLevel", "Con il grado 0 il livello va da 1 a 20.");
        assertThat(problemsOf(5, 1, 40, 1)).containsEntry("currentLevel", "Con il grado 1 il livello va da 20 a 40.");
        assertThat(problemsOf(39, 2, 40, 2)).containsEntry("currentLevel", "Con il grado 2 il livello è 40.");
        assertThat(problemsOf(1, 3, 40, 2)).containsEntry("currentRank", "Il grado va da 0 a 2.");
        assertThat(problemsOf(30, 1, 25, 1))
                .containsEntry("targetLevel", "L'obiettivo non può essere più basso della situazione attuale.");
    }

    @Test
    void lastRankGoesUpToTheMaximumLevelOfTheGame() {
        // The game raises the maximum level above the last cap: the last rank follows it
        GameRules raised = new GameRules(new Regeneration(240, 6), new Regeneration(480, 12), List.of(20, 40), 50,
                new BigDecimal("0.35"), 4, LocalDate.of(2026, 10, 1), List.of(30, 60, 120));
        RosterRules raisedRules = new RosterRules(raised);

        assertThat(raisedRules.problems(new Progress(40, 2, 50, 2))).isEmpty();
        assertThat(raisedRules.problems(new Progress(40, 2, 51, 2))).containsOnlyKeys("targetLevel");
        assertThat(raisedRules.problems(new Progress(39, 2, 50, 2)))
                .containsEntry("currentLevel", "Con il grado 2 il livello va da 40 a 50.");
    }

    @Test
    void requireValidThrowsOneErrorWithAllTheFields() {
        assertThatCode(() -> rules.requireValid(new Progress(1, 0, 40, 2))).doesNotThrowAnyException();

        assertThatThrownBy(() -> rules.requireValid(new Progress(21, 0, 41, 2)))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFields())
                        .containsOnlyKeys("currentLevel", "targetLevel"));
    }
}
