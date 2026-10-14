package com.resonanceplanner.inventory;

import com.resonanceplanner.catalog.MaterialRepository;
import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.inventory.dto.InventoryItemResponse;
import com.resonanceplanner.inventory.dto.UpdateQuantityRequest;
import com.resonanceplanner.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The inventory of the user of the token (D21, D44). It starts empty: only the materials the user has saved are
 * listed (D106). The token stays valid until it expires (D45), so every method answers 401 if its user does not
 * exist anymore (D102): otherwise the foreign key would answer with a database error.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository, MaterialRepository materialRepository,
            UserRepository userRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
    }

    public List<InventoryItemResponse> list(Long userId) {
        requireUser(userId);
        return inventoryItemRepository.findAllByUserIdOrderByMaterialIdAsc(userId).stream()
                .map(InventoryItemResponse::from)
                .toList();
    }

    /** Sets how many units of a material the user has, replacing the previous quantity. */
    @Transactional
    public InventoryItemResponse setQuantity(Long userId, String materialId, UpdateQuantityRequest request) {
        requireUser(userId);
        if (!materialRepository.existsById(materialId)) {
            throw new NotFoundException("Questo materiale non esiste.");
        }
        inventoryItemRepository.saveQuantity(userId, materialId, request.quantity());
        return new InventoryItemResponse(materialId, request.quantity());
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException();
        }
    }
}
