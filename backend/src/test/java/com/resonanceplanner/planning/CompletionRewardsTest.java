package com.resonanceplanner.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompletionRewardsTest {

    // The drops of "Simulation Training: Resonance Potion" in farming-sources.json
    private static final List<Drop> POTIONS = List.of(
            drop("premium-resonance-potion", "1.10"),
            drop("advanced-resonance-potion", "5.04"),
            drop("medium-resonance-potion", "2.35"),
            drop("shell-credit", "3600"));

    @Nested
    class BoostedRuns {

        @ParameterizedTest(name = "{0} run, limite {1}, già fatte {2} → {3} con il moltiplicatore")
        @CsvSource({
                "5, 3, 0, 3",   // only the first runs of the day, up to the limit
                "2, 3, 0, 2",   // fewer runs than the limit: all of them
                "5, 3, 2, 1",   // the runs already done that day use up the limit first
                "5, 3, 3, 0",   // limit already reached
                "5, 3, 7, 0",   // more than the limit already done: never a negative number
                "5,  , 9, 5",   // no daily limit: every run
        })
        void areTheFirstRunsOfTheDayUpToTheDailyLimit(int runs, Integer dailyLimit, long alreadyDone, int boosted) {
            assertThat(CompletionRewards.boostedRuns(runs, dailyLimit, alreadyDone)).isEqualTo(boosted);
        }
    }

    @Nested
    class Gained {

        @Test
        void isTheAverageQuantityOfEveryRunWithoutAnEvent() {
            Map<String, Long> gained = CompletionRewards.gained(POTIONS, 3, 0, BigDecimal.ONE);

            // 3.30, 15.12, 7.05 and 10800: rounded down
            assertThat(gained).containsExactly(
                    Map.entry("premium-resonance-potion", 3L),
                    Map.entry("advanced-resonance-potion", 15L),
                    Map.entry("medium-resonance-potion", 7L),
                    Map.entry("shell-credit", 10800L));
        }

        @Test
        void multipliesOnlyTheBoostedRuns() {
            // 3 runs, 2 of them doubled: as if they were 5 runs
            Map<String, Long> gained = CompletionRewards.gained(POTIONS, 3, 2, new BigDecimal("2"));

            // 5.50, 25.20, 11.75 and 18000
            assertThat(gained).containsExactly(
                    Map.entry("premium-resonance-potion", 5L),
                    Map.entry("advanced-resonance-potion", 25L),
                    Map.entry("medium-resonance-potion", 11L),
                    Map.entry("shell-credit", 18000L));
        }

        @Test
        void roundsDownTheTotalOfTheTaskNotEveryRun() {
            // 3 runs of 0.50: 1.50 in total gives 1, while rounding every run would give 0
            Map<String, Long> gained = CompletionRewards.gained(List.of(drop("coriolus", "0.50")), 3, 0,
                    BigDecimal.ONE);

            assertThat(gained).containsExactly(Map.entry("coriolus", 1L));
        }

        @Test
        void acceptsAMultiplierWithDecimals() {
            Map<String, Long> gained = CompletionRewards.gained(List.of(drop("coriolus", "2")), 1, 1,
                    new BigDecimal("1.5"));

            assertThat(gained).containsExactly(Map.entry("coriolus", 3L));
        }

        @Test
        void leavesOutAMaterialThatRoundsDownToZero() {
            Map<String, Long> gained = CompletionRewards.gained(
                    List.of(drop("coriolus", "0.30"), drop("shell-credit", "100")), 1, 0, BigDecimal.ONE);

            assertThat(gained).containsExactly(Map.entry("shell-credit", 100L));
        }

        @Test
        void refusesImpossibleRuns() {
            assertThatThrownBy(() -> CompletionRewards.gained(POTIONS, 0, 0, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CompletionRewards.gained(POTIONS, 2, 3, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CompletionRewards.gained(POTIONS, 2, -1, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Strongest {

        @Test
        void isTheEffectOnTheTypeOfTheSourceWithTheHighestMultiplier() {
            Effect boss = effect(FarmingSourceType.BOSS_CHALLENGE, "3", null);
            Effect doubleSim = effect(FarmingSourceType.SIMULATION_TRAINING, "2", 3);
            Effect tripleSim = effect(FarmingSourceType.SIMULATION_TRAINING, "3", 1);

            assertThat(CompletionRewards.strongest(List.of(boss, doubleSim, tripleSim),
                    FarmingSourceType.SIMULATION_TRAINING)).contains(tripleSim);
        }

        @Test
        void prefersTheHigherDailyLimitWhenTheMultiplierIsTheSame() {
            Effect three = effect(FarmingSourceType.SIMULATION_TRAINING, "2", 3);
            Effect unlimited = effect(FarmingSourceType.SIMULATION_TRAINING, "2", null);
            Effect one = effect(FarmingSourceType.SIMULATION_TRAINING, "2", 1);

            assertThat(CompletionRewards.strongest(List.of(three, unlimited, one),
                    FarmingSourceType.SIMULATION_TRAINING)).contains(unlimited);
            assertThat(CompletionRewards.strongest(List.of(one, three),
                    FarmingSourceType.SIMULATION_TRAINING)).contains(three);
        }

        @Test
        void isEmptyWhenNoEffectIsOnThatType() {
            assertThat(CompletionRewards.strongest(List.of(effect(FarmingSourceType.BOSS_CHALLENGE, "2", 3)),
                    FarmingSourceType.SIMULATION_TRAINING)).isEmpty();
            assertThat(CompletionRewards.strongest(List.of(), FarmingSourceType.SIMULATION_TRAINING)).isEmpty();
        }
    }

    private static Drop drop(String materialId, String avgQuantity) {
        return new Drop(materialId, new BigDecimal(avgQuantity), false);
    }

    private static Effect effect(FarmingSourceType type, String multiplier, Integer dailyLimit) {
        return new Effect(EffectType.DROP_MULTIPLIER, type, new BigDecimal(multiplier), dailyLimit);
    }
}
