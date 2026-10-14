package com.resonanceplanner.farming;

import com.resonanceplanner.catalog.MaterialCategory;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import com.resonanceplanner.farming.FarmingCatalog.MaterialInfo;
import com.resonanceplanner.farming.FarmingCatalog.Source;
import com.resonanceplanner.farming.FarmingPlan.MaterialLine;
import com.resonanceplanner.farming.FarmingPlan.SourceLine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * From a need and the inventory to the runs, Waveplate, minutes and days that are missing (D112). Each material
 * comes from the source that gives the most of it in a run; the materials are calculated in a fixed order and every
 * run subtracts all that it gives, by-products too. The averages have at most two decimals (D98), so the quantities
 * are counted in hundredths: the calculation is exact, with no floating point.
 */
public class FarmingCalculator {

    /** The id of the EXP line: the EXP is not a material of the catalog, but a sum of potions (D112, point 2). */
    public static final String EXP_ID = "character-exp";
    static final String EXP_NAME = "EXP personaggio";

    // D112, point 4: the sources whose by-products (potions, credits) help the others come first
    private static final List<MaterialCategory> ORDER = List.of(MaterialCategory.BOSS_DROP,
            MaterialCategory.ENEMY_DROP, MaterialCategory.LOCAL_SPECIALTY, MaterialCategory.CHARACTER_EXP,
            MaterialCategory.CREDIT);
    private static final int CENTS_DIGITS = 2;
    private static final long CENTS = 100;

    private final FarmingCatalog catalog;
    private final Map<String, Source> sourcesById = new HashMap<>();
    // What one run of each source gives, in hundredths, by material id and by EXP_ID
    private final Map<String, Map<String, Long>> centsPerRun = new HashMap<>();

    public FarmingCalculator(FarmingCatalog catalog) {
        this.catalog = catalog;
        for (Source source : catalog.sources()) {
            sourcesById.put(source.id(), source);
            centsPerRun.put(source.id(), yieldOf(source));
        }
    }

    public FarmingPlan plan(Need need, Map<String, Long> inventory, Integer playTimeMinutes) {
        List<MaterialLine> materials = materialLines(need, inventory);
        List<SourceLine> sources = sourceLines(runsBySource(materials));
        long waveplates = 0;
        long minutes = 0;
        for (SourceLine line : sources) {
            waveplates = Math.addExact(waveplates, line.waveplates());
            minutes = Math.addExact(minutes, line.minutes());
        }
        boolean containsEstimates = sources.stream()
                .flatMap(line -> sourcesById.get(line.farmingSourceId()).drops().stream())
                .anyMatch(Drop::estimated);
        return new FarmingPlan(materials, sources, waveplates, minutes,
                estimatedDays(waveplates, minutes, playTimeMinutes), containsEstimates);
    }

    // The EXP line, then every material with a quantity, in the order of the calculation
    private List<MaterialLine> materialLines(Need need, Map<String, Long> inventory) {
        List<MaterialLine> lines = new ArrayList<>();
        if (need.exp() > 0) {
            lines.add(line(EXP_ID, EXP_NAME, MaterialCategory.CHARACTER_EXP, need.exp(), ownedExp(inventory)));
        }
        need.materials().forEach((materialId, required) -> {
            if (required > 0) {
                MaterialInfo material = material(materialId);
                lines.add(line(materialId, material.name(), material.category(), required,
                        inventory.getOrDefault(materialId, 0L)));
            }
        });
        lines.sort(Comparator.comparingInt((MaterialLine line) -> orderOf(line.category()))
                .thenComparing(MaterialLine::materialId));
        return lines;
    }

    private static MaterialLine line(String id, String name, MaterialCategory category, long required, long owned) {
        return new MaterialLine(id, name, category, required, owned, Math.max(0, required - owned));
    }

    // Every Resonance Potion of the inventory counts for its EXP
    private long ownedExp(Map<String, Long> inventory) {
        long exp = 0;
        for (MaterialInfo material : catalog.materials().values()) {
            if (isPotion(material)) {
                exp = Math.addExact(exp, Math.multiplyExact(inventory.getOrDefault(material.id(), 0L), material.exp()));
            }
        }
        return exp;
    }

    // Runs by source id, in the order the sources were chosen
    private Map<String, Long> runsBySource(List<MaterialLine> materials) {
        Map<String, Long> left = missingInCents(materials);
        Map<String, Long> runs = new LinkedHashMap<>();
        for (MaterialLine line : materials) {
            long missing = left.getOrDefault(line.materialId(), 0L);
            if (missing == 0) {
                continue;
            }
            Source source = bestSourceOf(line.materialId());
            Map<String, Long> yield = centsPerRun.get(source.id());
            long sourceRuns = Math.ceilDiv(missing, yield.get(line.materialId()));
            runs.merge(source.id(), sourceRuns, Math::addExact);
            // Everything the runs give lowers what is still missing, by-products included
            left.replaceAll((materialId, cents) ->
                    Math.max(0, cents - Math.multiplyExact(sourceRuns, yield.getOrDefault(materialId, 0L))));
        }
        return runs;
    }

    private static Map<String, Long> missingInCents(List<MaterialLine> materials) {
        Map<String, Long> missing = new HashMap<>();
        for (MaterialLine line : materials) {
            if (line.missing() > 0) {
                missing.put(line.materialId(), Math.multiplyExact(line.missing(), CENTS));
            }
        }
        return missing;
    }

    // The most per run; on a tie the first id, so the result never depends on the order of the catalog
    private Source bestSourceOf(String materialId) {
        Comparator<Source> best = Comparator
                .comparingLong((Source source) -> centsPerRun.get(source.id()).getOrDefault(materialId, 0L))
                .reversed()
                .thenComparing(Source::id);
        return catalog.sources().stream()
                .filter(source -> centsPerRun.get(source.id()).getOrDefault(materialId, 0L) > 0)
                .min(best)
                .orElseThrow(() -> new IllegalStateException("No farming source gives " + materialId));
    }

    private List<SourceLine> sourceLines(Map<String, Long> runs) {
        List<SourceLine> lines = new ArrayList<>();
        runs.forEach((sourceId, sourceRuns) -> {
            Source source = sourcesById.get(sourceId);
            lines.add(new SourceLine(sourceId, source.name(), sourceRuns,
                    Math.multiplyExact(sourceRuns, source.waveplateCost()),
                    Math.multiplyExact(sourceRuns, source.minutesPerRun())));
        });
        return lines;
    }

    // The slower of the two: the Waveplate that regenerate in a day and the minutes the user plays (D112, point 6)
    private Long estimatedDays(long waveplates, long minutes, Integer playTimeMinutes) {
        if (playTimeMinutes == null) {
            return null;
        }
        return Math.max(Math.ceilDiv(waveplates, catalog.waveplatesPerDay()), Math.ceilDiv(minutes, playTimeMinutes));
    }

    private Map<String, Long> yieldOf(Source source) {
        Map<String, Long> yield = new HashMap<>();
        for (Drop drop : source.drops()) {
            long cents = centsOf(source, drop);
            yield.merge(drop.materialId(), cents, Math::addExact);
            MaterialInfo material = catalog.materials().get(drop.materialId());
            if (material != null && isPotion(material)) {
                yield.merge(EXP_ID, Math.multiplyExact(cents, material.exp()), Math::addExact);
            }
        }
        return Map.copyOf(yield);
    }

    // The schema keeps two decimals (D98): a third one is an error of the data, named here instead of a bare failure
    private static long centsOf(Source source, Drop drop) {
        try {
            return drop.avgQuantity().movePointRight(CENTS_DIGITS).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalStateException("The average drop of " + drop.materialId() + " in " + source.id()
                    + " has more than " + CENTS_DIGITS + " decimals: " + drop.avgQuantity(), e);
        }
    }

    private static boolean isPotion(MaterialInfo material) {
        return material.category() == MaterialCategory.CHARACTER_EXP && material.exp() != null;
    }

    private MaterialInfo material(String materialId) {
        MaterialInfo material = catalog.materials().get(materialId);
        if (material == null) {
            throw new IllegalStateException("The catalog has no material " + materialId);
        }
        return material;
    }

    private static int orderOf(MaterialCategory category) {
        int order = ORDER.indexOf(category);
        if (order < 0) {
            throw new IllegalStateException("The farming calculation has no place for the category " + category);
        }
        return order;
    }
}
