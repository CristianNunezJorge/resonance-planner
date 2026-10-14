package com.resonanceplanner.farming;

import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.farming.dto.CharacterRequirementsResponse;
import com.resonanceplanner.farming.dto.FarmingPlanResponse;
import com.resonanceplanner.inventory.InventoryItem;
import com.resonanceplanner.inventory.InventoryItemRepository;
import com.resonanceplanner.roster.OwnedCharacter;
import com.resonanceplanner.roster.OwnedCharacterRepository;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The farming calculation for the user of the token (D3, D44, D112): this class only reads the data, the numbers
 * come from {@link NeedCalculator} and {@link FarmingCalculator}. If the user of the token does not exist anymore
 * it answers 401 (D102).
 */
@Service
@Transactional(readOnly = true)
public class FarmingService {

    private static final String NOT_IN_ROSTER = "Questo personaggio non è nel tuo roster.";

    private final UserRepository userRepository;
    private final OwnedCharacterRepository ownedCharacterRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final FarmingCatalogLoader catalogLoader;

    public FarmingService(UserRepository userRepository, OwnedCharacterRepository ownedCharacterRepository,
            InventoryItemRepository inventoryItemRepository, FarmingCatalogLoader catalogLoader) {
        this.userRepository = userRepository;
        this.ownedCharacterRepository = ownedCharacterRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.catalogLoader = catalogLoader;
    }

    /** The whole roster together: the needs are summed and the inventory is counted once (D112, point 7). */
    public FarmingPlanResponse planFor(Long userId) {
        User user = requireUser(userId);
        FarmingCatalog catalog = catalogLoader.load();
        NeedCalculator needs = new NeedCalculator(catalog);
        Need need = Need.NONE;
        for (OwnedCharacter owned : ownedCharacterRepository.findAllByUserIdOrderByIdAsc(userId)) {
            need = need.plus(needs.needOf(owned.getCharacterId(), owned.getProgress()));
        }
        FarmingPlan plan = new FarmingCalculator(catalog).plan(need, inventoryOf(userId), user.getPlayTimeMinutes());
        return FarmingPlanResponse.from(plan);
    }

    /** One character, as if the whole inventory were for it; 404 if it is not in the roster of this user. */
    public CharacterRequirementsResponse requirementsFor(Long userId, String characterId) {
        User user = requireUser(userId);
        OwnedCharacter owned = ownedCharacterRepository.findByUserIdAndCharacterId(userId, characterId)
                .orElseThrow(() -> new NotFoundException(NOT_IN_ROSTER));
        FarmingCatalog catalog = catalogLoader.load();
        Need need = new NeedCalculator(catalog).needOf(owned.getCharacterId(), owned.getProgress());
        FarmingPlan plan = new FarmingCalculator(catalog).plan(need, inventoryOf(userId), user.getPlayTimeMinutes());
        return CharacterRequirementsResponse.from(owned.getCharacterId(), plan);
    }

    private Map<String, Long> inventoryOf(Long userId) {
        Map<String, Long> inventory = new HashMap<>();
        for (InventoryItem item : inventoryItemRepository.findAllByUserIdOrderByMaterialIdAsc(userId)) {
            inventory.put(item.getMaterialId(), (long) item.getQuantity());
        }
        return inventory;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
    }
}
