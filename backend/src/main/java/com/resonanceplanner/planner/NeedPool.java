package com.resonanceplanner.planner;

import com.resonanceplanner.farming.FarmingYields;
import com.resonanceplanner.farming.Need;
import com.resonanceplanner.farming.NeedCalculator;
import com.resonanceplanner.planner.PlannerInput.CharacterGoal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What every character still misses while the planner works (D123, point 1), in hundredths of a unit like
 * {@link FarmingYields}. The inventory is given to the characters in order of priority, and so is everything a run
 * gives. It is the working state of one calculation: it changes run after run and is never shared.
 */
class NeedPool {

    private final FarmingYields yields;
    // By character, in order of priority; inside a character by material, in the order of D112
    private final Map<String, Map<String, Long>> missing = new LinkedHashMap<>();
    private final Map<String, Long> initialMissing = new HashMap<>();

    NeedPool(List<CharacterGoal> characters, Map<String, Long> inventory, FarmingYields yields) {
        this.yields = yields;
        NeedCalculator needs = new NeedCalculator(yields.catalog());
        Map<String, Long> left = new HashMap<>(inventory);
        left.put(FarmingYields.EXP_ID, yields.ownedExp(inventory));
        for (CharacterGoal character : characters) {
            Need need = needs.needOf(character.characterId(), character.progress());
            Map<String, Long> required = new HashMap<>(need.materials());
            if (need.exp() > 0) {
                required.put(FarmingYields.EXP_ID, need.exp());
            }
            missing.put(character.characterId(), missingOf(required, left));
        }
    }

    /** The material ids that were missing at the start, in the order of the calculation (D112). */
    List<String> initialMaterials() {
        return initialMissing.keySet().stream().sorted(materialOrder()).toList();
    }

    /** The units of a material that were missing at the start, for every character together. */
    long initialMissing(String materialId) {
        return initialMissing.getOrDefault(materialId, 0L);
    }

    /** The units of a material that are still missing, rounded up. */
    long stillMissing(String materialId) {
        long cents = 0;
        for (Map<String, Long> materials : missing.values()) {
            cents = Math.addExact(cents, materials.getOrDefault(materialId, 0L));
        }
        return Math.ceilDiv(cents, FarmingYields.CENTS);
    }

    /** Every material a character still misses, in order of priority: first character first. */
    List<Target> targets() {
        List<Target> targets = new ArrayList<>();
        missing.forEach((characterId, materials) -> materials.forEach((materialId, cents) -> {
            if (cents > 0) {
                targets.add(new Target(characterId, materialId, cents));
            }
        }));
        return targets;
    }

    /** Gives what the runs produced to the characters in order of priority. */
    void add(Map<String, Long> gainedCents) {
        gainedCents.forEach((materialId, gained) -> {
            long left = gained;
            for (Map<String, Long> materials : missing.values()) {
                long needed = materials.getOrDefault(materialId, 0L);
                long used = Math.min(needed, left);
                if (used > 0) {
                    materials.put(materialId, needed - used);
                    left -= used;
                }
            }
        });
    }

    // The part of each material that the inventory still left does not cover; the inventory shrinks as it is given
    private Map<String, Long> missingOf(Map<String, Long> required, Map<String, Long> left) {
        Map<String, Long> materials = new LinkedHashMap<>();
        required.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey(materialOrder()))
                .forEach(entry -> {
                    String materialId = entry.getKey();
                    long owned = Math.min(left.getOrDefault(materialId, 0L), entry.getValue());
                    left.put(materialId, left.getOrDefault(materialId, 0L) - owned);
                    long units = entry.getValue() - owned;
                    if (units > 0) {
                        materials.put(materialId, Math.multiplyExact(units, FarmingYields.CENTS));
                        initialMissing.merge(materialId, units, Math::addExact);
                    }
                });
        return materials;
    }

    private Comparator<String> materialOrder() {
        return Comparator.comparingInt((String materialId) -> FarmingYields.orderOf(yields.categoryOf(materialId)))
                .thenComparing(Comparator.naturalOrder());
    }

    /** A material that a character still misses, in hundredths. */
    record Target(String characterId, String materialId, long missingCents) {}
}
