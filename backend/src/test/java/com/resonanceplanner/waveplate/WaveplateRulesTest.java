package com.resonanceplanner.waveplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.testsupport.TestGameRules;
import com.resonanceplanner.user.WaveplateSnapshot;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// The counter of the game (D28): 1 Waveplate every 6 minutes up to 240, then 1 Crystal every 12 minutes up to 480
class WaveplateRulesTest {

    private static final Instant RECORDED = Instant.parse("2026-10-01T12:00:00Z");

    private final WaveplateRules rules = new WaveplateRules(TestGameRules.standard());

    private static Instant after(Duration duration) {
        return RECORDED.plus(duration);
    }

    private WaveplateState at(int value, int crystals, Duration elapsed) {
        return rules.stateAt(new WaveplateSnapshot(value, crystals, RECORDED), after(elapsed));
    }

    @Test
    void addsOneWaveplateEverySixMinutesBelowTheMaximum() {
        WaveplateState state = at(100, 0, Duration.ofMinutes(60));

        assertThat(state.value()).isEqualTo(110);
        assertThat(state.crystals()).isZero();
        // 140 Waveplate are missing: 140 x 6 minutes after the reading
        assertThat(state.fullAt()).isEqualTo(after(Duration.ofMinutes(840)));
    }

    @Test
    void doesNotCountAnUnfinishedStep() {
        assertThat(at(100, 0, Duration.ofMinutes(5).plusSeconds(59)).value()).isEqualTo(100);
        assertThat(at(100, 0, Duration.ofMinutes(6)).value()).isEqualTo(101);
    }

    @Test
    void showsTheValueOfTheReadingAtTheMomentOfTheReading() {
        WaveplateState state = at(180, 0, Duration.ZERO);

        assertThat(state.value()).isEqualTo(180);
        assertThat(state.crystals()).isZero();
    }

    @Test
    void stopsAtTheMaximumWithoutCrystalsAtTheExactMoment() {
        WaveplateState state = at(230, 0, Duration.ofMinutes(60));

        assertThat(state.value()).isEqualTo(240);
        assertThat(state.crystals()).isZero();
        assertThat(state.fullAt()).isEqualTo(after(Duration.ofMinutes(60)));
    }

    @Test
    void startsToAccumulateCrystalsOnceTheWaveplateAreFull() {
        WaveplateState state = at(240, 0, Duration.ofMinutes(120));

        assertThat(state.value()).isEqualTo(240);
        assertThat(state.crystals()).isEqualTo(10);
        assertThat(state.fullAt()).isEqualTo(RECORDED);
        // 480 Crystals x 12 minutes
        assertThat(state.crystalsFullAt()).isEqualTo(after(Duration.ofMinutes(5760)));
    }

    @Test
    void countsTheCrystalsOnlyFromTheMomentTheWaveplateGotFull() {
        // Full after 60 minutes, then 36 minutes of Crystals: 3
        WaveplateState state = at(230, 0, Duration.ofMinutes(60 + 36));

        assertThat(state.value()).isEqualTo(240);
        assertThat(state.crystals()).isEqualTo(3);
        assertThat(state.crystalsFullAt()).isEqualTo(after(Duration.ofMinutes(60 + 5760)));
    }

    @Test
    void addsTheCrystalsToThoseOfTheReading() {
        assertThat(at(240, 100, Duration.ofMinutes(24)).crystals()).isEqualTo(102);
    }

    @Test
    void stopsAtTheMaximumOfTheCrystals() {
        WaveplateState state = at(240, 470, Duration.ofHours(1000));

        assertThat(state.crystals()).isEqualTo(480);
        // The 10 missing Crystals took 120 minutes: a moment in the past
        assertThat(state.crystalsFullAt()).isEqualTo(after(Duration.ofMinutes(120)));
    }

    @Test
    void neverGoesBackIfTheClockIsBeforeTheReading() {
        WaveplateState state = at(100, 0, Duration.ofMinutes(-30));

        assertThat(state.value()).isEqualTo(100);
        assertThat(state.crystals()).isZero();
    }

    @Test
    void keepsTheCrystalsOfTheReadingWhileTheWaveplateAreNotFull() {
        // 100 Waveplate and 300 Crystals together: the Crystals stay while the Waveplate are spent and refilled (D105)
        assertThat(at(100, 300, Duration.ofMinutes(60)).crystals()).isEqualTo(300);
        // Full after 140 x 6 minutes; then 24 minutes of Crystals: 2 more
        assertThat(at(100, 300, Duration.ofMinutes(840 + 24)).crystals()).isEqualTo(302);
    }

    @Test
    void countsACrystalOnlyWhenItsStepIsFinished() {
        assertThat(at(240, 0, Duration.ofMinutes(11).plusSeconds(59)).crystals()).isZero();
        assertThat(at(240, 0, Duration.ofMinutes(12)).crystals()).isEqualTo(1);
    }

    @Test
    void reachesTheMaximumOfTheCrystalsOnTheLastStep() {
        assertThat(at(240, 479, Duration.ofMinutes(11)).crystals()).isEqualTo(479);
        assertThat(at(240, 479, Duration.ofMinutes(12)).crystals()).isEqualTo(480);
    }

    @Test
    void neverShowsMoreThanTheMaximumEvenForAReadingSavedWithOtherRules() {
        WaveplateState state = at(300, 600, Duration.ofMinutes(10));

        assertThat(state.value()).isEqualTo(240);
        assertThat(state.crystals()).isEqualTo(480);
        // Already full before the reading: the "full at" moments are not after it
        assertThat(state.fullAt()).isEqualTo(RECORDED);
        assertThat(state.crystalsFullAt()).isEqualTo(RECORDED);
    }

    @ParameterizedTest
    @CsvSource({"0,0", "100,0", "239,0", "240,0", "240,1", "240,480", "100,300", "239,480"})
    void acceptsAReadingThatIsPossibleInTheGame(int value, int crystals) {
        assertThatCode(() -> rules.requireValidReading(value, crystals)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"241,0,value", "-1,0,value", "240,481,crystals", "240,-1,crystals", "100,481,crystals"})
    void rejectsAReadingThatIsNotPossibleAndNamesTheField(int value, int crystals, String field) {
        assertThatThrownBy(() -> rules.requireValidReading(value, crystals))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(error -> assertThat(((FieldValidationException) error).getFields()).containsOnlyKeys(field));
    }
}
