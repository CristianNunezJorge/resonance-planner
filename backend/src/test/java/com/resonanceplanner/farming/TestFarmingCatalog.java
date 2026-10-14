package com.resonanceplanner.farming;

import com.resonanceplanner.catalog.MaterialCategory;
import com.resonanceplanner.farming.FarmingCatalog.Ascension;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import com.resonanceplanner.farming.FarmingCatalog.MaterialInfo;
import com.resonanceplanner.farming.FarmingCatalog.Source;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A catalog for the unit tests: materials, sources and ascensions of Jinhsi and Yinlin with the real values of the
 * JSON files, but a simple EXP table (1000 EXP for every level), so the expected numbers are easy to check by hand.
 */
final class TestFarmingCatalog {

    static final String CREDIT = "shell-credit";

    private TestFarmingCatalog() {}

    static FarmingCatalog standard() {
        return withSources(standardSources());
    }

    static FarmingCatalog withSources(List<Source> sources) {
        Map<Integer, Integer> expToNext = new HashMap<>();
        for (int level = 1; level < 40; level++) {
            expToNext.put(level, 1000);
        }
        return new FarmingCatalog(expToNext, ascensions(), materials(), sources, new BigDecimal("0.35"), 240);
    }

    static FarmingCatalog withExpToNext(Map<Integer, Integer> expToNext) {
        return new FarmingCatalog(expToNext, ascensions(), materials(), standardSources(), new BigDecimal("0.35"), 240);
    }

    static List<Source> standardSources() {
        return List.of(
                source("sim-resonance-potion", 40, 3,
                        drop("premium-resonance-potion", "1.10"), drop("advanced-resonance-potion", "5.04"),
                        drop("medium-resonance-potion", "2.35"), drop(CREDIT, "3600")),
                source("sim-shell-credit", 40, 3, drop(CREDIT, "72000")),
                source("boss-mourning-aix", 60, 4,
                        drop("elegy-tacet-core", "2.0"), drop("advanced-resonance-potion", "2.18"), drop(CREDIT, "5760")),
                source("boss-mech-abomination", 60, 4,
                        drop("group-abomination-tacet-core", "2.0"), drop("advanced-resonance-potion", "2.18"),
                        drop(CREDIT, "5760")),
                source("overworld-howlers", 0, 10, estimated("lf-howler-core", "5"), estimated("mf-howler-core", "2")),
                source("overworld-whisperins", 0, 10,
                        estimated("lf-whisperin-core", "5"), estimated("mf-whisperin-core", "2")),
                source("collect-loongs-pearl", 0, 10, estimated("loongs-pearl", "6")),
                source("collect-coriolus", 0, 10, estimated("coriolus", "6")));
    }

    static Source source(String id, int waveplateCost, int minutesPerRun, Drop... drops) {
        return new Source(id, "Fonte " + id, waveplateCost, minutesPerRun, List.of(drops));
    }

    static Drop drop(String materialId, String avgQuantity) {
        return new Drop(materialId, new BigDecimal(avgQuantity), false);
    }

    static Drop estimated(String materialId, String avgQuantity) {
        return new Drop(materialId, new BigDecimal(avgQuantity), true);
    }

    private static Map<String, List<Ascension>> ascensions() {
        return Map.of(
                "jinhsi", List.of(
                        new Ascension(1, Map.of(CREDIT, 5000, "lf-howler-core", 4)),
                        new Ascension(2, Map.of(CREDIT, 10000, "elegy-tacet-core", 3, "loongs-pearl", 4,
                                "mf-howler-core", 4))),
                "yinlin", List.of(
                        new Ascension(1, Map.of(CREDIT, 5000, "lf-whisperin-core", 4)),
                        new Ascension(2, Map.of(CREDIT, 10000, "group-abomination-tacet-core", 3, "coriolus", 4,
                                "mf-whisperin-core", 4))));
    }

    private static Map<String, MaterialInfo> materials() {
        return List.of(
                        new MaterialInfo(CREDIT, "Shell Credit", MaterialCategory.CREDIT, null),
                        potion("basic-resonance-potion", "Basic Resonance Potion", 1000),
                        potion("medium-resonance-potion", "Medium Resonance Potion", 3000),
                        potion("advanced-resonance-potion", "Advanced Resonance Potion", 8000),
                        potion("premium-resonance-potion", "Premium Resonance Potion", 20000),
                        material("lf-howler-core", "LF Howler Core", MaterialCategory.ENEMY_DROP),
                        material("mf-howler-core", "MF Howler Core", MaterialCategory.ENEMY_DROP),
                        material("lf-whisperin-core", "LF Whisperin Core", MaterialCategory.ENEMY_DROP),
                        material("mf-whisperin-core", "MF Whisperin Core", MaterialCategory.ENEMY_DROP),
                        material("elegy-tacet-core", "Elegy Tacet Core", MaterialCategory.BOSS_DROP),
                        material("group-abomination-tacet-core", "Group Abomination Tacet Core",
                                MaterialCategory.BOSS_DROP),
                        material("loongs-pearl", "Loong's Pearl", MaterialCategory.LOCAL_SPECIALTY),
                        material("coriolus", "Coriolus", MaterialCategory.LOCAL_SPECIALTY))
                .stream()
                .collect(Collectors.toMap(MaterialInfo::id, material -> material));
    }

    private static MaterialInfo potion(String id, String name, int exp) {
        return new MaterialInfo(id, name, MaterialCategory.CHARACTER_EXP, exp);
    }

    private static MaterialInfo material(String id, String name, MaterialCategory category) {
        return new MaterialInfo(id, name, category, null);
    }
}
