package com.resonanceplanner.farming;

import static com.resonanceplanner.farming.FarmingCalculator.EXP_ID;
import static com.resonanceplanner.farming.TestFarmingCatalog.CREDIT;
import static com.resonanceplanner.farming.TestFarmingCatalog.estimated;
import static com.resonanceplanner.farming.TestFarmingCatalog.source;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.resonanceplanner.catalog.MaterialCategory;
import com.resonanceplanner.farming.FarmingPlan.MaterialLine;
import com.resonanceplanner.farming.FarmingPlan.SourceLine;
import com.resonanceplanner.roster.Progress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Runs, Waveplate, minutes and days (D112) with the real drops of the JSON files
class FarmingCalculatorTest {

    private static final int ONE_HOUR = 60;

    private final FarmingCalculator calculator = new FarmingCalculator(TestFarmingCatalog.standard());

    // What Jinhsi needs from level 1 to 40 with the real EXP table (208,800 EXP): the example of D112
    private static final Need JINHSI_TO_THE_CAP = new Need(208_800, Map.of(
            "elegy-tacet-core", 3L, "lf-howler-core", 4L, "mf-howler-core", 4L, "loongs-pearl", 4L,
            CREDIT, 73_080L + 5_000 + 10_000));

    private static Map<String, Long> materials(Object... idsAndQuantities) {
        Map<String, Long> materials = new HashMap<>();
        for (int i = 0; i < idsAndQuantities.length; i += 2) {
            materials.put((String) idsAndQuantities[i], ((Number) idsAndQuantities[i + 1]).longValue());
        }
        return materials;
    }

    @Test
    void plansOneCharacterFromTheStartToTheCap() {
        FarmingPlan plan = calculator.plan(JINHSI_TO_THE_CAP, Map.of(), ONE_HOUR);

        // Boss first: its potions and credits lower the runs of EXP and credits that come later
        assertThat(plan.sources())
                .extracting(SourceLine::farmingSourceId, SourceLine::runs, SourceLine::waveplates, SourceLine::minutes)
                .containsExactly(
                        tuple("boss-mourning-aix", 2L, 120L, 8L),
                        tuple("overworld-howlers", 2L, 0L, 20L),
                        tuple("collect-loongs-pearl", 1L, 0L, 10L),
                        tuple("sim-resonance-potion", 3L, 120L, 9L),
                        tuple("sim-shell-credit", 1L, 40L, 3L));
        assertThat(plan.waveplates()).isEqualTo(280);
        assertThat(plan.minutes()).isEqualTo(50);
        // 280 Waveplate need 2 days of regeneration, while 50 minutes fit in one hour of play
        assertThat(plan.estimatedDays()).isEqualTo(2);
        assertThat(plan.containsEstimates()).isTrue();
    }

    @Test
    void listsEveryNeededMaterialInTheOrderOfTheCalculation() {
        FarmingPlan plan = calculator.plan(JINHSI_TO_THE_CAP, Map.of(), ONE_HOUR);

        assertThat(plan.materials())
                .extracting(MaterialLine::materialId, MaterialLine::category, MaterialLine::required,
                        MaterialLine::owned, MaterialLine::missing)
                .containsExactly(
                        tuple("elegy-tacet-core", MaterialCategory.BOSS_DROP, 3L, 0L, 3L),
                        tuple("lf-howler-core", MaterialCategory.ENEMY_DROP, 4L, 0L, 4L),
                        tuple("mf-howler-core", MaterialCategory.ENEMY_DROP, 4L, 0L, 4L),
                        tuple("loongs-pearl", MaterialCategory.LOCAL_SPECIALTY, 4L, 0L, 4L),
                        tuple(EXP_ID, MaterialCategory.CHARACTER_EXP, 208_800L, 0L, 208_800L),
                        tuple(CREDIT, MaterialCategory.CREDIT, 88_080L, 0L, 88_080L));
        assertThat(plan.materials()).extracting(MaterialLine::name)
                .containsExactly("Elegy Tacet Core", "LF Howler Core", "MF Howler Core", "Loong's Pearl",
                        "EXP personaggio", "Shell Credit");
        assertThat(plan.completionPercent()).isZero();
    }

    @Test
    void plansSeveralCharactersAndCountsTheInventoryOnce() {
        NeedCalculator needs = new NeedCalculator(TestFarmingCatalog.standard());
        Need both = needs.needOf("jinhsi", new Progress(1, 0, 40, 2))
                .plus(needs.needOf("yinlin", new Progress(1, 0, 40, 2)));
        // One Premium Resonance Potion: 20,000 EXP for both characters together, not for each one
        Map<String, Long> inventory = materials("premium-resonance-potion", 1);

        FarmingPlan plan = calculator.plan(both, inventory, 30);

        assertThat(plan.materials()).filteredOn(line -> line.materialId().equals(EXP_ID))
                .extracting(MaterialLine::required, MaterialLine::owned, MaterialLine::missing)
                .containsExactly(tuple(78_000L, 20_000L, 58_000L));
        // The two bosses give 69,760 EXP: no run of Resonance Potions is needed anymore
        assertThat(plan.sources())
                .extracting(SourceLine::farmingSourceId, SourceLine::runs)
                .containsExactly(
                        tuple("boss-mourning-aix", 2L),
                        tuple("boss-mech-abomination", 2L),
                        tuple("overworld-howlers", 2L),
                        tuple("overworld-whisperins", 2L),
                        tuple("collect-coriolus", 1L),
                        tuple("collect-loongs-pearl", 1L),
                        tuple("sim-shell-credit", 1L));
        assertThat(plan.waveplates()).isEqualTo(280);
        assertThat(plan.minutes()).isEqualTo(79);
        // Now the minutes are the slower part: 79 minutes with 30 a day are 3 days, the Waveplate only 2
        assertThat(plan.estimatedDays()).isEqualTo(3);
    }

    @Test
    void needsNothingWhenTheGoalIsReached() {
        FarmingPlan plan = calculator.plan(Need.NONE, materials(CREDIT, 500), ONE_HOUR);

        assertThat(plan.materials()).isEmpty();
        assertThat(plan.sources()).isEmpty();
        assertThat(plan.waveplates()).isZero();
        assertThat(plan.minutes()).isZero();
        assertThat(plan.estimatedDays()).isZero();
        assertThat(plan.containsEstimates()).isFalse();
        assertThat(plan.completionPercent()).isEqualTo(100);
    }

    @Test
    void needsNoRunWhenTheInventoryCoversEverything() {
        Need need = new Need(0, materials("lf-howler-core", 4, CREDIT, 5_000));

        FarmingPlan plan = calculator.plan(need, materials("lf-howler-core", 10, CREDIT, 5_000), ONE_HOUR);

        assertThat(plan.materials())
                .extracting(MaterialLine::materialId, MaterialLine::owned, MaterialLine::missing)
                .containsExactly(tuple("lf-howler-core", 10L, 0L), tuple(CREDIT, 5_000L, 0L));
        assertThat(plan.sources()).isEmpty();
        assertThat(plan.estimatedDays()).isZero();
        assertThat(plan.completionPercent()).isEqualTo(100);
    }

    @Test
    void countsThePotionsOfTheInventoryForTheirExp() {
        Need need = new Need(50_000, Map.of());
        // 2 × 20,000 + 3 × 1,000 = 43,000 EXP; a material that is not needed changes nothing
        Map<String, Long> inventory = materials("premium-resonance-potion", 2, "basic-resonance-potion", 3,
                "coriolus", 99);

        FarmingPlan plan = calculator.plan(need, inventory, ONE_HOUR);

        assertThat(plan.materials())
                .extracting(MaterialLine::materialId, MaterialLine::required, MaterialLine::owned, MaterialLine::missing)
                .containsExactly(tuple(EXP_ID, 50_000L, 43_000L, 7_000L));
        assertThat(plan.sources()).extracting(SourceLine::farmingSourceId, SourceLine::runs)
                .containsExactly(tuple("sim-resonance-potion", 1L));
    }

    @Test
    void letsTheByProductsOfTheBossLowerTheCreditRuns() {
        Need need = new Need(0, materials("elegy-tacet-core", 3, CREDIT, 80_000));

        FarmingPlan plan = calculator.plan(need, Map.of(), ONE_HOUR);

        // 2 boss runs give 11,520 credits: 68,480 are left, one run of 72,000 instead of two
        assertThat(plan.sources()).extracting(SourceLine::farmingSourceId, SourceLine::runs)
                .containsExactly(tuple("boss-mourning-aix", 2L), tuple("sim-shell-credit", 1L));
    }

    @Test
    void countsASourceThatGivesTwoNeededMaterialsOnce() {
        Need need = new Need(0, materials("lf-howler-core", 4, "mf-howler-core", 4));

        FarmingPlan plan = calculator.plan(need, Map.of(), ONE_HOUR);

        // One run gives 5 LF and 2 MF; the second run gives the other 2 MF
        assertThat(plan.sources()).extracting(SourceLine::farmingSourceId, SourceLine::runs)
                .containsExactly(tuple("overworld-howlers", 2L));
    }

    @Test
    void roundsTheRunsUp() {
        FarmingPlan plan = calculator.plan(new Need(0, materials("lf-howler-core", 1)), Map.of(), ONE_HOUR);

        assertThat(plan.sources()).extracting(SourceLine::runs, SourceLine::minutes).containsExactly(tuple(1L, 10L));
        assertThat(plan.estimatedDays()).isEqualTo(1);
    }

    @Test
    void hasNoEstimatedDaysWithoutAPlayTime() {
        FarmingPlan plan = calculator.plan(JINHSI_TO_THE_CAP, Map.of(), null);

        assertThat(plan.estimatedDays()).isNull();
        assertThat(plan.waveplates()).isEqualTo(280);
    }

    @Test
    void containsNoEstimatesWhenOnlyVerifiedDropsAreUsed() {
        FarmingPlan plan = calculator.plan(new Need(0, materials(CREDIT, 1_000)), Map.of(), ONE_HOUR);

        assertThat(plan.sources()).extracting(SourceLine::farmingSourceId).containsExactly("sim-shell-credit");
        assertThat(plan.containsEstimates()).isFalse();
    }

    @Test
    void averagesTheOwnedPartOfEveryMaterialRoundingDown() {
        Need need = new Need(0, materials("lf-howler-core", 4, "elegy-tacet-core", 3));

        // 2 of 4 = 50; 1 of 3 = 33; the average of 83 is 41
        assertThat(calculator.plan(need, materials("lf-howler-core", 2, "elegy-tacet-core", 1), ONE_HOUR)
                .completionPercent()).isEqualTo(41);
        // More than needed counts as all of it, not more: 100 and 0 give 50
        assertThat(calculator.plan(need, materials("lf-howler-core", 8), ONE_HOUR).completionPercent()).isEqualTo(50);
    }

    @Test
    void picksTheSourceThatGivesTheMostPerRunAndTheFirstIdOnATie() {
        Need need = new Need(0, materials("lf-howler-core", 5));
        FarmingCalculator tie = new FarmingCalculator(TestFarmingCatalog.withSources(List.of(
                source("b-source", 0, 10, estimated("lf-howler-core", "5")),
                source("a-source", 0, 10, estimated("lf-howler-core", "5")),
                source("c-source", 0, 10, estimated("lf-howler-core", "3")))));
        FarmingCalculator most = new FarmingCalculator(TestFarmingCatalog.withSources(List.of(
                source("a-source", 0, 10, estimated("lf-howler-core", "5")),
                source("c-source", 0, 10, estimated("lf-howler-core", "6.5")))));

        assertThat(tie.plan(need, Map.of(), ONE_HOUR).sources()).extracting(SourceLine::farmingSourceId)
                .containsExactly("a-source");
        assertThat(most.plan(need, Map.of(), ONE_HOUR).sources()).extracting(SourceLine::farmingSourceId)
                .containsExactly("c-source");
    }

    @Test
    void refusesAMaterialThatNoSourceGives() {
        FarmingCalculator noBoss = new FarmingCalculator(TestFarmingCatalog.withSources(List.of(
                source("overworld-howlers", 0, 10, estimated("lf-howler-core", "5")))));

        // A data error must not look like a material that is free
        assertThatThrownBy(() -> noBoss.plan(new Need(0, materials("elegy-tacet-core", 3)), Map.of(), ONE_HOUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("elegy-tacet-core");
        assertThatThrownBy(() -> noBoss.plan(new Need(1_000, Map.of()), Map.of(), ONE_HOUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EXP_ID);
    }

    @Test
    void addsTwoDropsOfTheSameMaterialInOneSource() {
        FarmingCalculator twoDrops = new FarmingCalculator(TestFarmingCatalog.withSources(List.of(
                source("overworld-howlers", 0, 10, estimated("lf-howler-core", "2"), estimated("lf-howler-core", "3")))));

        FarmingPlan plan = twoDrops.plan(new Need(0, materials("lf-howler-core", 5)), Map.of(), ONE_HOUR);

        assertThat(plan.sources()).extracting(SourceLine::runs).containsExactly(1L);
    }

    @Test
    void refusesAnAverageWithMoreThanTwoDecimals() {
        List<FarmingCatalog.Source> threeDecimals = List.of(
                source("overworld-howlers", 0, 10, estimated("lf-howler-core", "5.005")));

        assertThatThrownBy(() -> new FarmingCalculator(TestFarmingCatalog.withSources(threeDecimals)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lf-howler-core")
                .hasMessageContaining("overworld-howlers");
    }

    @Test
    void refusesAMaterialThatIsNotInTheCatalog() {
        assertThatThrownBy(() -> calculator.plan(new Need(0, materials("mystery-core", 1)), Map.of(), ONE_HOUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mystery-core");
    }

    @Test
    void handlesTheLargestInventoryWithoutOverflowing() {
        long most = 999_999_999L;
        Map<String, Long> inventory = materials("basic-resonance-potion", most, "medium-resonance-potion", most,
                "advanced-resonance-potion", most, "premium-resonance-potion", most, CREDIT, most);

        FarmingPlan plan = calculator.plan(JINHSI_TO_THE_CAP, inventory, ONE_HOUR);

        assertThat(plan.materials()).filteredOn(line -> line.materialId().equals(EXP_ID))
                .extracting(MaterialLine::owned, MaterialLine::missing)
                .containsExactly(tuple(most * 32_000, 0L));
        assertThat(plan.sources()).extracting(SourceLine::farmingSourceId)
                .doesNotContain("sim-resonance-potion", "sim-shell-credit");
    }
}
