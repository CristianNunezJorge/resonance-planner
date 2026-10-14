package com.resonanceplanner.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The inventory of a user. It extends the bare Repository on purpose: JpaRepository would bring findAll, findById and
 * deleteById, which take no user, and a later card could call them by mistake (D44). Every method here takes the user.
 */
public interface InventoryItemRepository extends Repository<InventoryItem, Long> {

    List<InventoryItem> findAllByUserIdOrderByMaterialIdAsc(Long userId);

    /**
     * Saves the quantity of a material, creating the row the first time, in one statement: with a check followed by an
     * insert, two requests for the same new material would both insert and one would hit the unique index.
     * Pending changes are written first, but the persistence context is not cleared: an InventoryItem that was already
     * read in the same transaction stays as it was, so read the inventory again in a new transaction after saving.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into inventory_item (user_id, material_id, quantity)
            values (:userId, :materialId, :quantity)
            on conflict (user_id, material_id) do update set quantity = excluded.quantity
            """, nativeQuery = true)
    void saveQuantity(@Param("userId") Long userId, @Param("materialId") String materialId,
            @Param("quantity") int quantity);
}
