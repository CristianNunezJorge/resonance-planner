package com.resonanceplanner.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** EXP needed to go from a level to the next one, the same for every character. */
@Entity
@Table(name = "level_exp")
public class LevelExp {

    @Id
    private Integer level;

    @Column(nullable = false)
    private Integer expToNext;

    protected LevelExp() {
        // for JPA
    }

    public LevelExp(Integer level) {
        this.level = level;
    }

    public void update(Integer expToNext) {
        this.expToNext = expToNext;
    }

    public Integer getLevel() {
        return level;
    }

    public Integer getExpToNext() {
        return expToNext;
    }
}
