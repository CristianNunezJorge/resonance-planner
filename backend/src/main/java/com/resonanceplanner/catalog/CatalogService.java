package com.resonanceplanner.catalog;

import com.resonanceplanner.catalog.dto.CharacterDetailResponse;
import com.resonanceplanner.catalog.dto.CharacterSummaryResponse;
import com.resonanceplanner.catalog.dto.FarmingSourceResponse;
import com.resonanceplanner.catalog.dto.GameRulesResponse;
import com.resonanceplanner.catalog.dto.MaterialResponse;
import com.resonanceplanner.common.error.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only access to the catalog (D49). The entities become DTOs inside the transaction,
 * because the lazy relations cannot be loaded after it ends (open-in-view is off).
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final GameCharacterRepository gameCharacterRepository;
    private final MaterialRepository materialRepository;
    private final FarmingSourceRepository farmingSourceRepository;
    private final GameRules gameRules;

    public CatalogService(GameCharacterRepository gameCharacterRepository, MaterialRepository materialRepository,
            FarmingSourceRepository farmingSourceRepository, GameRules gameRules) {
        this.gameCharacterRepository = gameCharacterRepository;
        this.materialRepository = materialRepository;
        this.farmingSourceRepository = farmingSourceRepository;
        this.gameRules = gameRules;
    }

    public List<CharacterSummaryResponse> listCharacters() {
        return gameCharacterRepository.findAllByOrderByNameAsc().stream().map(CharacterSummaryResponse::from).toList();
    }

    public CharacterDetailResponse getCharacter(String id) {
        return gameCharacterRepository.findWithAscensionCostsById(id)
                .map(character -> CharacterDetailResponse.from(character, gameRules.ascensionLevels()))
                .orElseThrow(() -> new NotFoundException("Personaggio non trovato."));
    }

    public List<MaterialResponse> listMaterials() {
        return materialRepository.findAllByOrderByCategoryAscNameAsc().stream().map(MaterialResponse::from).toList();
    }

    public List<FarmingSourceResponse> listFarmingSources() {
        return farmingSourceRepository.findAllByOrderByTypeAscNameAsc().stream().map(FarmingSourceResponse::from).toList();
    }

    public GameRulesResponse getRules() {
        return GameRulesResponse.from(gameRules);
    }

    /** Id → name of every character, for the files that show names instead of ids (D133). */
    public Map<String, String> characterNames() {
        return gameCharacterRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toUnmodifiableMap(GameCharacter::getId, GameCharacter::getName));
    }

    /** Id → name of every farming source, without loading the drops (D133). */
    public Map<String, String> farmingSourceNames() {
        return farmingSourceRepository.findAllByOrderByTypeAscNameAsc().stream()
                .collect(Collectors.toUnmodifiableMap(FarmingSource::getId, FarmingSource::getName));
    }
}
