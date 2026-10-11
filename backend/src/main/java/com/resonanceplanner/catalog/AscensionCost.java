package com.resonanceplanner.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One material of one ascension: the ascension to rank 1 of Jinhsi needs 5000 Shell Credit, for example (D34). */
@Entity
@Table(name = "ascension_cost")
public class AscensionCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    // The ascension that brings the character to this rank
    @Column(nullable = false)
    private Integer toRank;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(nullable = false)
    private Integer quantity;

    protected AscensionCost() {
        // for JPA
    }

    public AscensionCost(GameCharacter character, Integer toRank, Material material, Integer quantity) {
        this.character = character;
        this.toRank = toRank;
        this.material = material;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public GameCharacter getCharacter() {
        return character;
    }

    public Integer getToRank() {
        return toRank;
    }

    public Material getMaterial() {
        return material;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
