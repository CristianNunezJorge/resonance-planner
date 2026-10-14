package com.resonanceplanner.farming;

import static com.resonanceplanner.farming.TestFarmingCatalog.CREDIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.roster.Progress;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// What a character needs (D112, point 1). In the test catalog every level costs 1000 EXP and 1 EXP costs 0.35 credits
class NeedCalculatorTest {

    private final NeedCalculator calculator = new NeedCalculator(TestFarmingCatalog.standard());

    private Need jinhsi(int currentLevel, int currentRank, int targetLevel, int targetRank) {
        return calculator.needOf("jinhsi", new Progress(currentLevel, currentRank, targetLevel, targetRank));
    }

    @Test
    void addsTheExpOfEveryLevelAndBothAscensionsFromTheStartToTheCap() {
        Need need = jinhsi(1, 0, 40, 2);

        // 39 levels of 1000 EXP; credits: 39000 × 0.35 for the levels, then 5000 and 10000 for the two ascensions
        assertThat(need.exp()).isEqualTo(39_000);
        assertThat(need.materials()).containsOnly(
                Map.entry(CREDIT, 13_650L + 5_000 + 10_000),
                Map.entry("lf-howler-core", 4L),
                Map.entry("elegy-tacet-core", 3L),
                Map.entry("loongs-pearl", 4L),
                Map.entry("mf-howler-core", 4L));
    }

    @Test
    void needsOnlyTheAscensionWhenTheLevelStaysTheSame() {
        Need need = jinhsi(20, 0, 20, 1);

        assertThat(need.exp()).isZero();
        assertThat(need.materials()).containsOnly(Map.entry(CREDIT, 5_000L), Map.entry("lf-howler-core", 4L));
    }

    @Test
    void needsOnlyTheLevelsWhenTheRankStaysTheSame() {
        Need need = jinhsi(20, 1, 40, 1);

        assertThat(need.exp()).isEqualTo(20_000);
        assertThat(need.materials()).containsOnly(Map.entry(CREDIT, 7_000L));
    }

    @Test
    void leavesOutTheAscensionAlreadyDone() {
        Need need = jinhsi(20, 1, 40, 2);

        assertThat(need.exp()).isEqualTo(20_000);
        assertThat(need.materials()).containsOnly(
                Map.entry(CREDIT, 7_000L + 10_000),
                Map.entry("elegy-tacet-core", 3L),
                Map.entry("loongs-pearl", 4L),
                Map.entry("mf-howler-core", 4L));
    }

    @Test
    void needsNothingWhenTheGoalIsAlreadyReached() {
        assertThat(jinhsi(40, 2, 40, 2)).isEqualTo(Need.NONE);
        assertThat(jinhsi(1, 0, 1, 0)).isEqualTo(Need.NONE);
    }

    @Test
    void roundsTheCreditsOfTheLevelsUp() {
        NeedCalculator oddTable = new NeedCalculator(TestFarmingCatalog.withExpToNext(Map.of(1, 401)));

        Need need = oddTable.needOf("jinhsi", new Progress(1, 0, 2, 0));

        // 401 × 0.35 = 140.35: a fraction of a credit cannot be paid
        assertThat(need.exp()).isEqualTo(401);
        assertThat(need.materials()).containsOnly(Map.entry(CREDIT, 141L));
    }

    @Test
    void refusesALevelThatIsMissingFromTheExpTable() {
        NeedCalculator holedTable = new NeedCalculator(TestFarmingCatalog.withExpToNext(Map.of(1, 400, 3, 500)));

        assertThatThrownBy(() -> holedTable.needOf("jinhsi", new Progress(1, 0, 4, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("level 2");
    }

    @Test
    void refusesAnAscensionThatIsMissingFromTheCatalog() {
        // Aalto is not in the test catalog: a goal with an ascension cannot be calculated, and must not count as free
        assertThatThrownBy(() -> calculator.needOf("aalto", new Progress(20, 0, 20, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aalto");
    }

    @Test
    void refusesACatalogWithoutExactlyOneCreditMaterial() {
        FarmingCatalog noCredit = new FarmingCatalog(Map.of(1, 400), Map.of(), Map.of(), List.of(), new BigDecimal("0.35"),
                240);

        assertThatThrownBy(() -> new NeedCalculator(noCredit).needOf("jinhsi", new Progress(1, 0, 2, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one CREDIT");
    }

    @Test
    void sumsTheNeedsOfTwoCharacters() {
        Need jinhsi = jinhsi(20, 0, 20, 1);
        Need yinlin = calculator.needOf("yinlin", new Progress(20, 1, 40, 1));

        Need both = jinhsi.plus(yinlin);

        assertThat(both.exp()).isEqualTo(20_000);
        assertThat(both.materials()).containsOnly(
                Map.entry(CREDIT, 5_000L + 7_000),
                Map.entry("lf-howler-core", 4L));
        assertThat(Need.NONE.plus(jinhsi)).isEqualTo(jinhsi);
    }

    @Test
    void refusesASumThatDoesNotFitInALong() {
        Need huge = new Need(Long.MAX_VALUE, Map.of(CREDIT, Long.MAX_VALUE));

        assertThatThrownBy(() -> huge.plus(new Need(1, Map.of()))).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> huge.plus(new Need(0, Map.of(CREDIT, 1L)))).isInstanceOf(ArithmeticException.class);
    }
}
