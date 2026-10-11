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
import java.math.BigDecimal;

/** Average quantity of a material that a farming source gives for each run. */
@Entity
@Table(name = "farming_drop")
public class FarmingDrop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farming_source_id")
    private FarmingSource farmingSource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(nullable = false)
    private BigDecimal avgQuantity;

    // true when the number is our own estimate (D25)
    @Column(nullable = false)
    private boolean estimated;

    protected FarmingDrop() {
        // for JPA
    }

    public FarmingDrop(FarmingSource farmingSource, Material material, BigDecimal avgQuantity, boolean estimated) {
        this.farmingSource = farmingSource;
        this.material = material;
        this.avgQuantity = avgQuantity;
        this.estimated = estimated;
    }

    public Long getId() {
        return id;
    }

    public FarmingSource getFarmingSource() {
        return farmingSource;
    }

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getAvgQuantity() {
        return avgQuantity;
    }

    public boolean isEstimated() {
        return estimated;
    }
}
