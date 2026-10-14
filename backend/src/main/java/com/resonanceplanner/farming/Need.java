package com.resonanceplanner.farming;

import java.util.HashMap;
import java.util.Map;

/**
 * What one or more characters need to reach their goal, before the inventory (D112): the EXP in points and the
 * other materials (Shell Credit included) by id. The quantities are {@code long} because they are summed (D108).
 */
public record Need(long exp, Map<String, Long> materials) {

    public static final Need NONE = new Need(0, Map.of());

    public Need {
        materials = Map.copyOf(materials);
    }

    /** The need of two characters together; it never overflows in silence. */
    public Need plus(Need other) {
        Map<String, Long> sum = new HashMap<>(materials);
        other.materials.forEach((materialId, quantity) -> sum.merge(materialId, quantity, Math::addExact));
        return new Need(Math.addExact(exp, other.exp), sum);
    }
}
