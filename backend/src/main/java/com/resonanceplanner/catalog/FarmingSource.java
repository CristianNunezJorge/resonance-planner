package com.resonanceplanner.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Where materials are obtained: a Simulation Training, a boss, enemies or collection in the world. */
@Entity
@Table(name = "farming_source")
public class FarmingSource {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmingSourceType type;

    @Column(nullable = false)
    private Integer waveplateCost;

    // Our own estimate (D31)
    @Column(nullable = false)
    private Integer minutesPerRun;

    // Read-only side: the drops are written through FarmingDropRepository
    @OneToMany(mappedBy = "farmingSource")
    @OrderBy("id")
    private List<FarmingDrop> drops = new ArrayList<>();

    protected FarmingSource() {
        // for JPA
    }

    public FarmingSource(String id) {
        this.id = id;
    }

    public void update(String name, FarmingSourceType type, Integer waveplateCost, Integer minutesPerRun) {
        this.name = name;
        this.type = type;
        this.waveplateCost = waveplateCost;
        this.minutesPerRun = minutesPerRun;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public FarmingSourceType getType() {
        return type;
    }

    public Integer getWaveplateCost() {
        return waveplateCost;
    }

    public Integer getMinutesPerRun() {
        return minutesPerRun;
    }

    public List<FarmingDrop> getDrops() {
        return Collections.unmodifiableList(drops);
    }
}
