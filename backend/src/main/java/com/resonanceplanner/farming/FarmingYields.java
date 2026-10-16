package com.resonanceplanner.farming;

import com.resonanceplanner.catalog.MaterialCategory;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import com.resonanceplanner.farming.FarmingCatalog.MaterialInfo;
import com.resonanceplanner.farming.FarmingCatalog.Source;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What every source gives in a run, and which source is the best for each material (D112, D123): the part of the
 * farming calculation that the planner shares. The averages have at most two decimals (D98), so the quantities are
 * counted in hundredths; the Resonance Potions also count as EXP, on the line {@link #EXP_ID}.
 */
public class FarmingYields {

    /** The id of the EXP line: the EXP is not a material of the catalog, but a sum of potions (D112, point 2). */
    public static final String EXP_ID = "character-exp";
    public static final String EXP_NAME = "EXP personaggio";
    /** The quantities are counted in hundredths of a unit. */
    public static final long CENTS = 100;

    // D112, point 4: the sources whose by-products (potions, credits) help the others come first
    private static final List<MaterialCategory> ORDER = List.of(MaterialCategory.BOSS_DROP,
            MaterialCategory.ENEMY_DROP, MaterialCategory.LOCAL_SPECIALTY, MaterialCategory.CHARACTER_EXP,
            MaterialCategory.CREDIT);
    private static final int CENTS_DIGITS = 2;

    private final FarmingCatalog catalog;
    private final Map<String, Source> sourcesById = new HashMap<>();
    // What one run of each source gives, in hundredths, by material id and by EXP_ID
    private final Map<String, Map<String, Long>> centsPerRun = new HashMap<>();

    public FarmingYields(FarmingCatalog catalog) {
        this.catalog = catalog;
        for (Source source : catalog.sources()) {
            sourcesById.put(source.id(), source);
            centsPerRun.put(source.id(), yieldOf(source));
        }
    }

    public FarmingCatalog catalog() {
        return catalog;
    }

    public Source source(String sourceId) {
        Source source = sourcesById.get(sourceId);
        if (source == null) {
            throw new IllegalStateException("The catalog has no farming source " + sourceId);
        }
        return source;
    }

    /** What one run of the source gives, in hundredths, by material id and by {@link #EXP_ID}. */
    public Map<String, Long> centsPerRun(String sourceId) {
        return centsPerRun.get(source(sourceId).id());
    }

    /** The source that gives the most of the material in a run; on a tie the first id, whatever the catalog order. */
    public Source bestSourceOf(String materialId) {
        Comparator<Source> best = Comparator
                .comparingLong((Source source) -> centsPerRun.get(source.id()).getOrDefault(materialId, 0L))
                .reversed()
                .thenComparing(Source::id);
        return catalog.sources().stream()
                .filter(source -> centsPerRun.get(source.id()).getOrDefault(materialId, 0L) > 0)
                .min(best)
                .orElseThrow(() -> new IllegalStateException("No farming source gives " + materialId));
    }

    /** Every Resonance Potion of the inventory counted as EXP points. */
    public long ownedExp(Map<String, Long> inventory) {
        long exp = 0;
        for (MaterialInfo material : catalog.materials().values()) {
            if (isPotion(material)) {
                exp = Math.addExact(exp, Math.multiplyExact(inventory.getOrDefault(material.id(), 0L), material.exp()));
            }
        }
        return exp;
    }

    /** A material of the catalog; a missing one is an error of the data. */
    public MaterialInfo material(String materialId) {
        MaterialInfo material = catalog.materials().get(materialId);
        if (material == null) {
            throw new IllegalStateException("The catalog has no material " + materialId);
        }
        return material;
    }

    /** The category of a line of the need: the EXP line is CHARACTER_EXP. */
    public MaterialCategory categoryOf(String materialId) {
        return EXP_ID.equals(materialId) ? MaterialCategory.CHARACTER_EXP : material(materialId).category();
    }

    /** The name of a line of the need. */
    public String nameOf(String materialId) {
        return EXP_ID.equals(materialId) ? EXP_NAME : material(materialId).name();
    }

    /** The place of a category in the order of the calculation (D112, point 4). */
    public static int orderOf(MaterialCategory category) {
        int order = ORDER.indexOf(category);
        if (order < 0) {
            throw new IllegalStateException("The farming calculation has no place for the category " + category);
        }
        return order;
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
}
