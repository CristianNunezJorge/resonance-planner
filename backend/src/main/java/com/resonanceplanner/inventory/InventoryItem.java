package com.resonanceplanner.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * How many units of a material a user has (D21). The user and the material are plain identifiers: the foreign keys of
 * the table guarantee that they exist. The class is only for reading: the quantity is written by one SQL statement
 * (InventoryItemRepository.saveQuantity), so two requests for the same new material cannot clash.
 */
@Entity
@Immutable
@Table(name = "inventory_item")
public class InventoryItem {

    /**
     * The most units of one material that can be saved. Two of these still fit in an int, three do not: a card that
     * adds quantities (the inventory with a need, a completed task) must sum in a long or with Math.addExact.
     */
    public static final int MAX_QUANTITY = 999_999_999;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 60)
    private String materialId;

    @Column(nullable = false)
    private int quantity;

    protected InventoryItem() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public int getQuantity() {
        return quantity;
    }
}
