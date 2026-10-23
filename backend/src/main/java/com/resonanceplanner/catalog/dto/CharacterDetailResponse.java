package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.AscensionCost;
import com.resonanceplanner.catalog.Element;
import com.resonanceplanner.catalog.GameCharacter;
import com.resonanceplanner.catalog.WeaponType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A character with the materials of each ascension (D34). */
public record CharacterDetailResponse(
        @Schema(example = "jinhsi") String id,
        @Schema(example = "Jinhsi") String name,
        @Schema(example = "SPECTRO") Element element,
        @Schema(example = "BROADBLADE") WeaponType weaponType,
        @Schema(description = "Stelle: 4 o 5", example = "5") Integer rarity,
        @Schema(example = "Main Damage Dealer") String role,
        @Schema(description = "Percorso dell'icona nel sito", example = "characters/jinhsi.webp") String icon,
        List<AscensionResponse> ascensions) {

    // atLevel: the level at which this ascension is done, from the game rules (D27)
    public record AscensionResponse(
            @Schema(description = "Il grado che si raggiunge", example = "2") int toRank,
            @Schema(description = "Il livello in cui si fa l'ascensione", example = "40") Integer atLevel,
            List<CostResponse> costs) {}

    public record CostResponse(
            @Schema(example = "elegy-tacet-core") String materialId,
            @Schema(example = "Elegy Tacet Core") String materialName,
            @Schema(example = "3") int quantity) {

        static CostResponse from(AscensionCost cost) {
            return new CostResponse(cost.getMaterial().getId(), cost.getMaterial().getName(), cost.getQuantity());
        }
    }

    public static CharacterDetailResponse from(GameCharacter character, List<Integer> ascensionLevels) {
        // The costs arrive ordered by rank: group them, keeping the order
        Map<Integer, List<CostResponse>> costsByRank = new LinkedHashMap<>();
        for (AscensionCost cost : character.getAscensionCosts()) {
            costsByRank.computeIfAbsent(cost.getToRank(), rank -> new ArrayList<>()).add(CostResponse.from(cost));
        }
        List<AscensionResponse> ascensions = costsByRank.entrySet().stream()
                .map(entry -> new AscensionResponse(entry.getKey(), levelOf(entry.getKey(), ascensionLevels),
                        List.copyOf(entry.getValue())))
                .toList();
        return new CharacterDetailResponse(character.getId(), character.getName(), character.getElement(),
                character.getWeaponType(), character.getRarity(), character.getRole(), character.getIcon(), ascensions);
    }

    // The ascension to rank 1 happens at the first ascension level, and so on
    private static Integer levelOf(int toRank, List<Integer> ascensionLevels) {
        return toRank >= 1 && toRank <= ascensionLevels.size() ? ascensionLevels.get(toRank - 1) : null;
    }
}
