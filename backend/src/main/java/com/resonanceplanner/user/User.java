package com.resonanceplanner.user;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import org.hibernate.annotations.DynamicUpdate;

/**
 * An account of the application. The table is called users because "user" is a reserved word of PostgreSQL.
 */
@Entity
@Table(name = "users")
// An UPDATE writes only the columns that changed: two requests that change different fields of the same user at the
// same moment (the profile, the guided setup, the Waveplate) must not cancel each other out
@DynamicUpdate
public class User {

    // Username of the account created at startup from ADMIN_EMAIL; nobody can register it (D100)
    public static final String ADMIN_USERNAME = "admin";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    // Always lowercase: the table has a CHECK for it, and it makes the login independent of letter case
    @Column(nullable = false, length = 254)
    private String email;

    // BCrypt hash, never the plain password (D45)
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    // Empty until the guided setup is complete (D16)
    private Integer playTimeMinutes;

    @Column(nullable = false)
    private boolean onboardingCompleted;

    // The last Waveplate reading (D36): null until the user saves the first one. The three columns are one value.
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "waveplate_value")),
        @AttributeOverride(name = "crystals", column = @Column(name = "waveplate_crystals")),
        @AttributeOverride(name = "recordedAt", column = @Column(name = "waveplate_recorded_at"))
    })
    private WaveplateSnapshot waveplate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
        // for JPA
    }

    private User(String username, String email, String passwordHash, Role role) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public static User create(String username, String email, String passwordHash, Role role) {
        return new User(username, normalizeEmail(email), passwordHash, role);
    }

    /** The form in which an email is saved and searched: no surrounding spaces, all lowercase. */
    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public void changeUsername(String username) {
        this.username = username;
    }

    public void changePlayTimeMinutes(int playTimeMinutes) {
        this.playTimeMinutes = playTimeMinutes;
    }

    public void recordWaveplate(WaveplateSnapshot waveplate) {
        this.waveplate = waveplate;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Integer getPlayTimeMinutes() {
        return playTimeMinutes;
    }

    public WaveplateSnapshot getWaveplate() {
        return waveplate;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
