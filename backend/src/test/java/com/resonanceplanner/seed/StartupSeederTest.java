package com.resonanceplanner.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EventActivity;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.GameEventRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Integration test on the test database: the seeder already ran at startup; every test is rolled back
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StartupSeederTest {

    @Autowired
    private StartupSeeder startupSeeder;

    @Autowired
    private CatalogSeeder catalogSeeder;

    @Autowired
    private DemoEventSeeder demoEventSeeder;

    @Autowired
    private SeedDataReader seedDataReader;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Test
    void loadsTheWholeCatalogAndTheDemoEventsAtStartup() {
        assertCatalogAndEventsMatchTheJsonFiles();
    }

    @Test
    void doesNotDuplicateAnythingWhenItRunsAgain() {
        startupSeeder.run(null);
        entityManager.flush();

        assertCatalogAndEventsMatchTheJsonFiles();
    }

    @Test
    void restoresTheCatalogValuesOfTheJsonFiles() {
        jdbc.update("UPDATE material SET name = 'Changed by hand' WHERE id = 'shell-credit'");
        jdbc.update("UPDATE game_character SET rarity = 4 WHERE id = 'jinhsi'");
        jdbc.update("""
                UPDATE farming_drop SET avg_quantity = 99
                WHERE farming_source_id = 'sim-resonance-potion' AND material_id = 'premium-resonance-potion'
                """);

        startupSeeder.run(null);
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT name FROM material WHERE id = 'shell-credit'", String.class))
                .isEqualTo("Shell Credit");
        assertThat(jdbc.queryForObject("SELECT rarity FROM game_character WHERE id = 'jinhsi'", Integer.class))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject("""
                SELECT avg_quantity FROM farming_drop
                WHERE farming_source_id = 'sim-resonance-potion' AND material_id = 'premium-resonance-potion'
                """, BigDecimal.class)).isEqualByComparingTo("1.10");
    }

    @Test
    void keepsTheChangesTheAdminMadeToADemoEvent() {
        jdbc.update("UPDATE game_event SET name = 'Edited by the admin' WHERE code = 'bountiful-crescendo'");

        startupSeeder.run(null);
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT name FROM game_event WHERE code = 'bountiful-crescendo'", String.class))
                .isEqualTo("Edited by the admin");
    }

    @Test
    void savesActivitiesRewardsAndEffectOfTheDemoEvents() {
        // Seed the demo events again inside this test, so the conversion runs on every test run
        jdbc.update("DELETE FROM game_event WHERE code IS NOT NULL");
        demoEventSeeder.seed(seedDataReader.read().events());
        entityManager.flush();
        entityManager.clear();

        GameEvent lamentRecon = gameEventRepository.findByCode("lament-recon-solaris-soldier").orElseThrow();
        GameEvent bountifulCrescendo = gameEventRepository.findByCode("bountiful-crescendo").orElseThrow();

        assertThat(lamentRecon.getActivities()).extracting(EventActivity::getTitle)
                .containsExactly("Drill 1", "Drill 2", "Drill 3", "Drill 4", "Drill 5", "Drill 6", "Drill 7");
        assertThat(lamentRecon.getRewards()).hasSize(2).first().asString().startsWith("Medaglia");
        assertThat(lamentRecon.getEffect()).isNull();
        assertThat(bountifulCrescendo.getEffect().getSourceType()).isEqualTo(FarmingSourceType.SIMULATION_TRAINING);
        assertThat(bountifulCrescendo.getEffect().getMultiplier()).isEqualByComparingTo("2");
        assertThat(bountifulCrescendo.getEffect().getDailyLimit()).isEqualTo(3);
        assertThat(bountifulCrescendo.getActiveWeekdays()).isEmpty();
    }

    @Test
    void savesTheActiveWeekdaysAndTheUnlockDayOfAnEvent() {
        SeedData.EventData weekendEvent = new SeedData.EventData("test-weekend-event", "Weekend event",
                EventType.POINTS_SHOP, "Only on weekends", new SeedData.ScheduleData(0, 14, 14,
                        List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
                List.of("Reward"), List.of(new SeedData.ActivityData("Weekend challenge", 10, ActivityFrequency.DAILY, 3)),
                null);

        demoEventSeeder.seed(List.of(weekendEvent));
        entityManager.flush();
        entityManager.clear();

        GameEvent saved = gameEventRepository.findByCode("test-weekend-event").orElseThrow();
        assertThat(saved.getActiveWeekdays()).containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(saved.getActivities()).singleElement()
                .satisfies(activity -> assertThat(activity.getUnlockDay()).isEqualTo(3));
    }

    // Runs outside the test transaction, to prove that a broken file leaves the catalog as it was
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void stopsWithAClearMessageAndChangesNothingWhenAFileReferencesAnUnknownMaterial() {
        SeedData data = seedDataReader.read();
        SeedData.FarmingSourceData first = data.farmingSources().getFirst();
        List<SeedData.FarmingSourceData> sources = new ArrayList<>(data.farmingSources());
        sources.set(0, new SeedData.FarmingSourceData(first.id(), first.name(), first.type(), first.waveplateCost(),
                first.minutesPerRun(), List.of(new SeedData.DropData("unknown-material", BigDecimal.ONE, false))));
        SeedData broken = new SeedData(data.materials(), sources, data.levels(), data.characters(), data.events());

        assertThatThrownBy(() -> catalogSeeder.seed(broken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown-material")
                .hasMessageContaining(first.id());
        assertCatalogAndEventsMatchTheJsonFiles();
    }

    private void assertCatalogAndEventsMatchTheJsonFiles() {
        SeedData data = seedDataReader.read();
        assertThat(count("material")).isEqualTo(data.materials().size());
        assertThat(count("farming_source")).isEqualTo(data.farmingSources().size());
        assertThat(count("farming_drop")).isEqualTo(
                data.farmingSources().stream().mapToInt(source -> source.drops().size()).sum());
        assertThat(count("level_exp")).isEqualTo(data.levels().size());
        assertThat(count("game_character")).isEqualTo(data.characters().size());
        assertThat(count("ascension_cost")).isEqualTo(data.characters().stream()
                .flatMap(character -> character.ascensions().stream())
                .mapToInt(ascension -> ascension.cost().size()).sum());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM game_event WHERE code IS NOT NULL", Integer.class))
                .isEqualTo(data.events().size());
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
