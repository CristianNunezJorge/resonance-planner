package com.resonanceplanner.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A game material. The id is the readable identifier of the JSON files, such as "lf-howler-core" (D32). */
@Entity
@Table(name = "material")
public class Material {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialCategory category;

    // Only for CHARACTER_EXP materials
    private Integer exp;

    private String icon;

    protected Material() {
        // for JPA
    }

    public Material(String id) {
        this.id = id;
    }

    /** Copies the values of the JSON file: used both for new and for existing materials (D39). */
    public void update(String name, MaterialCategory category, Integer exp, String icon) {
        this.name = name;
        this.category = category;
        this.exp = exp;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MaterialCategory getCategory() {
        return category;
    }

    public Integer getExp() {
        return exp;
    }

    public String getIcon() {
        return icon;
    }
}
