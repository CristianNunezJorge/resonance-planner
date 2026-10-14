package com.resonanceplanner.farming;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.catalog.GameCharacter;
import com.resonanceplanner.catalog.GameCharacterRepository;
import com.resonanceplanner.farming.FarmingPlan.MaterialLine;
import com.resonanceplanner.roster.Progress;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// The real catalog loaded from the JSON files: every character can be calculated from the start to the cap, so a
// missing ascension, EXP level or farming source is found here and not by a user as a 500 (D112)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FarmingCatalogLoaderTest {

    @Autowired
    private FarmingCatalogLoader loader;

    @Autowired
    private GameCharacterRepository gameCharacterRepository;

    @Test
    void readsTheRealRulesAndEveryAscension() {
        FarmingCatalog catalog = loader.load();

        assertThat(catalog.waveplatesPerDay()).isEqualTo(240);
        assertThat(catalog.expToNext()).hasSize(39);
        assertThat(catalog.creditMaterialId()).isEqualTo("shell-credit");
        assertThat(catalog.ascensions().get("jinhsi"))
                .extracting(FarmingCatalog.Ascension::toRank)
                .containsExactly(1, 2);
        assertThat(catalog.ascensions().get("jinhsi").get(0).cost())
                .isEqualTo(Map.of("shell-credit", 5000, "lf-howler-core", 4));
    }

    @Test
    void plansEveryCharacterOfTheCatalogFromTheStartToTheCap() {
        FarmingCatalog catalog = loader.load();
        NeedCalculator needs = new NeedCalculator(catalog);
        FarmingCalculator calculator = new FarmingCalculator(catalog);
        List<GameCharacter> characters = gameCharacterRepository.findAll();

        assertThat(characters).hasSize(6);
        for (GameCharacter character : characters) {
            Need need = needs.needOf(character.getId(), new Progress(1, 0, 40, 2));

            FarmingPlan plan = calculator.plan(need, Map.of(), 60);

            // EXP, credits, two enemy drops, a boss core and a specialty, each with a source that gives it
            assertThat(plan.materials()).as(character.getId()).hasSize(6);
            assertThat(plan.materials()).extracting(MaterialLine::missing).allMatch(missing -> missing > 0);
            assertThat(plan.sources()).as(character.getId()).isNotEmpty();
            assertThat(plan.estimatedDays()).as(character.getId()).isPositive();
        }
    }
}
