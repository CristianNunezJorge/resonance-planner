package com.resonanceplanner.farming;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.catalog.MaterialCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The part of the catalog that the farming calculation needs (D112), as plain values: the calculation works on it
 * without the database.
 *
 * @param expToNext EXP to go from each level to the next one
 * @param ascensions the ascensions of each character, by character id
 * @param materials every material, by id
 * @param sources every farming source, with its average drops
 * @param waveplatesPerDay the Waveplate that regenerate in a day (240)
 */
public record FarmingCatalog(
        Map<Integer, Integer> expToNext,
        Map<String, List<Ascension>> ascensions,
        Map<String, MaterialInfo> materials,
        List<Source> sources,
        BigDecimal shellCreditPerExp,
        int waveplatesPerDay) {

    // Read-only copies, the lists inside the map too; a day without Waveplate would make every estimate divide by zero
    public FarmingCatalog {
        expToNext = Map.copyOf(expToNext);
        ascensions = ascensions.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        materials = Map.copyOf(materials);
        sources = List.copyOf(sources);
        if (waveplatesPerDay <= 0) {
            throw new IllegalArgumentException("At least one Waveplate must regenerate in a day: " + waveplatesPerDay);
        }
    }

    /** The only material of the CREDIT category: Shell Credit pays for every level (D93). */
    public String creditMaterialId() {
        List<String> credits = materials.values().stream()
                .filter(material -> material.category() == MaterialCategory.CREDIT)
                .map(MaterialInfo::id)
                .toList();
        if (credits.size() != 1) {
            throw new IllegalStateException("The catalog must have exactly one CREDIT material, found " + credits);
        }
        return credits.getFirst();
    }

    /** A material; {@code exp} is set only for the Resonance Potions (CHARACTER_EXP). */
    public record MaterialInfo(String id, String name, MaterialCategory category, Integer exp) {}

    /** The cost of reaching an ascension rank, as quantity by material id. */
    public record Ascension(int toRank, Map<String, Integer> cost) {

        public Ascension {
            cost = Map.copyOf(cost);
        }
    }

    /** Where materials come from: what a run costs and what it gives on average; the type is what events boost (D94). */
    public record Source(String id, String name, FarmingSourceType type, int waveplateCost, int minutesPerRun,
            List<Drop> drops) {

        public Source {
            drops = List.copyOf(drops);
        }
    }

    /** The average quantity of a material in one run, with at most two decimals (D98). */
    public record Drop(String materialId, BigDecimal avgQuantity, boolean estimated) {}
}
