package com.resonanceplanner.planner;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.event.dto.EventResponse.Activity;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.planner.PlannerInput.CharacterGoal;
import com.resonanceplanner.roster.Progress;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

// The fingerprint of the data a plan was made from (D125): the same data give the same fingerprint, any change of
// roster, goals or events gives another one
class PlanInputsTest {

    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 14);
    private static final CharacterGoal JINHSI = new CharacterGoal("jinhsi", new Progress(1, 0, 40, 2));
    private static final CharacterGoal YINLIN = new CharacterGoal("yinlin", new Progress(20, 1, 40, 2));

    private static EventResponse event(String multiplier, int activityMinutes) {
        return new EventResponse(7L, "Festa", EventType.DOUBLE_DROP, "Descrizione", MONDAY, MONDAY.plusDays(6), null,
                List.of(MONDAY, MONDAY.plusDays(1)), List.of("Pozioni"),
                List.of(new Activity("Drill 1", activityMinutes, ActivityFrequency.ONCE, null)),
                new Effect(EffectType.DROP_MULTIPLIER, FarmingSourceType.SIMULATION_TRAINING, new BigDecimal(multiplier),
                        3));
    }

    private static String fingerprint(List<CharacterGoal> roster, List<EventResponse> events) {
        return PlanInputs.fingerprintOf(roster, events);
    }

    @Test
    void isAHexSha256AndTheSameForTheSameData() {
        String first = fingerprint(List.of(JINHSI, YINLIN), List.of(event("2", 15)));

        assertThat(first).hasSize(64).matches("[0-9a-f]+");
        assertThat(fingerprint(List.of(JINHSI, YINLIN), List.of(event("2", 15)))).isEqualTo(first);
    }

    @Test
    void doesNotDependOnTheOrderOfTheRosterNorOnTheZerosOfAMultiplier() {
        assertThat(fingerprint(List.of(YINLIN, JINHSI), List.of(event("2.00", 15))))
                .isEqualTo(fingerprint(List.of(JINHSI, YINLIN), List.of(event("2", 15))));
    }

    @Test
    void changesWithAGoal() {
        CharacterGoal higherGoal = new CharacterGoal("jinhsi", new Progress(1, 0, 40, 3));

        assertThat(fingerprint(List.of(higherGoal), List.of()))
                .isNotEqualTo(fingerprint(List.of(JINHSI), List.of()));
    }

    @Test
    void changesWithTheRoster() {
        assertThat(fingerprint(List.of(JINHSI, YINLIN), List.of()))
                .isNotEqualTo(fingerprint(List.of(JINHSI), List.of()));
    }

    @Test
    void changesWithTheEvents() {
        String before = fingerprint(List.of(JINHSI), List.of(event("2", 15)));

        assertThat(fingerprint(List.of(JINHSI), List.of(event("2", 20)))).isNotEqualTo(before);
        assertThat(fingerprint(List.of(JINHSI), List.of(event("1.5", 15)))).isNotEqualTo(before);
        assertThat(fingerprint(List.of(JINHSI), List.of())).isNotEqualTo(before);
    }

    @Test
    void writesAMissingValueApartFromTheText() {
        EventResponse withoutEffect = new EventResponse(7L, "Festa", EventType.POINTS_SHOP, "", MONDAY, MONDAY, null,
                List.of(MONDAY), List.of(), List.of(new Activity("null", 15, ActivityFrequency.DAILY, null)), null);

        assertThat(fingerprint(List.of(), List.of(withoutEffect))).hasSize(64)
                .isNotEqualTo(fingerprint(List.of(), List.of(event("2", 15))));
    }

    @Test
    void cannotBeFooledByASeparatorInsideAName() {
        // A name that contains what could look like the end of a field and the start of the next one
        CharacterGoal tricky = new CharacterGoal("jinhsi\n1:x", new Progress(1, 0, 40, 2));

        assertThat(fingerprint(List.of(tricky), List.of())).isNotEqualTo(fingerprint(List.of(JINHSI), List.of()));
    }

    @Test
    void doesNotMixUpTwoFieldsThatTouch() {
        // "ab" + "c" and "a" + "bc" must not give the same text
        CharacterGoal ab = new CharacterGoal("ab", new Progress(1, 0, 40, 2));
        CharacterGoal a = new CharacterGoal("a", new Progress(11, 0, 40, 2));

        assertThat(fingerprint(List.of(ab), List.of())).isNotEqualTo(fingerprint(List.of(a), List.of()));
    }
}
