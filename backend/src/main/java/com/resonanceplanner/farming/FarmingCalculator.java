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
 * are counted in hundredths ({@link FarmingYields}): the calculation is exact, with no floating point.
 */
public class FarmingCalculator {

    /** The id of the EXP line: the EXP is not a material of the catalog, but a sum of potions (D112, point 2). */
    public static final String EXP_ID = FarmingYields.EXP_ID;

    private final FarmingCatalog catalog;
    private final FarmingYields yields;

    public FarmingCalculator(FarmingCatalog catalog) {
        this.catalog = catalog;
        this.yields = new FarmingYields(catalog);
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
                .flatMap(line -> yields.source(line.farmingSourceId()).drops().stream())
                .anyMatch(Drop::estimated);
        return new FarmingPlan(materials, sources, waveplates, minutes,
                estimatedDays(waveplates, minutes, playTimeMinutes), containsEstimates);
    }

    // The EXP line, then every material with a quantity, in the order of the calculation
    private List<MaterialLine> materialLines(Need need, Map<String, Long> inventory) {
        List<MaterialLine> lines = new ArrayList<>();
        if (need.exp() > 0) {
            lines.add(line(EXP_ID, FarmingYields.EXP_NAME, MaterialCategory.CHARACTER_EXP, need.exp(),
                    yields.ownedExp(inventory)));
        }
        need.materials().forEach((materialId, required) -> {
            if (required > 0) {
                MaterialInfo material = yields.material(materialId);
                lines.add(line(materialId, material.name(), material.category(), required,
                        inventory.getOrDefault(materialId, 0L)));
            }
        });
        lines.sort(Comparator.comparingInt((MaterialLine line) -> FarmingYields.orderOf(line.category()))
                .thenComparing(MaterialLine::materialId));
        return lines;
    }

    private static MaterialLine line(String id, String name, MaterialCategory category, long required, long owned) {
        return new MaterialLine(id, name, category, required, owned, Math.max(0, required - owned));
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
            Source source = yields.bestSourceOf(line.materialId());
            Map<String, Long> yield = yields.centsPerRun(source.id());
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
                missing.put(line.materialId(), Math.multiplyExact(line.missing(), FarmingYields.CENTS));
            }
        }
        return missing;
    }

    private List<SourceLine> sourceLines(Map<String, Long> runs) {
        List<SourceLine> lines = new ArrayList<>();
        runs.forEach((sourceId, sourceRuns) -> {
            Source source = yields.source(sourceId);
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
}
