package com.resonanceplanner.roster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A character in the roster of a user, with level and rank now and as a goal (D34). The user and the character are
 * kept as plain identifiers: the foreign keys of the table guarantee that they exist, and the roster never needs
 * more than that (the name and the icon come from the public catalog).
 */
@Entity
@Table(name = "owned_character")
public class OwnedCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false, length = 60)
    private String characterId;

    @Column(nullable = false)
    private int currentLevel;

    @Column(nullable = false)
    private int currentRank;

    @Column(nullable = false)
    private int targetLevel;

    @Column(nullable = false)
    private int targetRank;

    protected OwnedCharacter() {
        // for JPA
    }

    public OwnedCharacter(Long userId, String characterId, Progress progress) {
        this.userId = userId;
        this.characterId = characterId;
        changeProgress(progress);
    }

    public void changeProgress(Progress progress) {
        this.currentLevel = progress.currentLevel();
        this.currentRank = progress.currentRank();
        this.targetLevel = progress.targetLevel();
        this.targetRank = progress.targetRank();
    }

    public Progress getProgress() {
        return new Progress(currentLevel, currentRank, targetLevel, targetRank);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCharacterId() {
        return characterId;
    }
}
