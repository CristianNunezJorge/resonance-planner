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

/** One of the six demo characters (D17). */
@Entity
@Table(name = "game_character")
public class GameCharacter {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Element element;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeaponType weaponType;

    @Column(nullable = false)
    private Integer rarity;

    @Column(nullable = false)
    private String role;

    // Path of the icon in the frontend, for example "characters/jinhsi.webp" (D30)
    private String icon;

    // Read-only side: the costs are written through AscensionCostRepository
    @OneToMany(mappedBy = "character")
    @OrderBy("toRank, id")
    private List<AscensionCost> ascensionCosts = new ArrayList<>();

    protected GameCharacter() {
        // for JPA
    }

    public GameCharacter(String id) {
        this.id = id;
    }

    public void update(String name, Element element, WeaponType weaponType, Integer rarity, String role, String icon) {
        this.name = name;
        this.element = element;
        this.weaponType = weaponType;
        this.rarity = rarity;
        this.role = role;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Element getElement() {
        return element;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public Integer getRarity() {
        return rarity;
    }

    public String getRole() {
        return role;
    }

    public String getIcon() {
        return icon;
    }

    public List<AscensionCost> getAscensionCosts() {
        return Collections.unmodifiableList(ascensionCosts);
    }
}
