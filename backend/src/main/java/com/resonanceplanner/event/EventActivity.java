package com.resonanceplanner.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Something an event asks the player to do, such as a combat drill. */
@Entity
@Table(name = "event_activity")
public class EventActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private GameEvent event;

    @Column(nullable = false)
    private String title;

    // Our estimate (D31)
    @Column(nullable = false)
    private Integer minutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityFrequency frequency;

    // Day of the event from which the activity is available; empty = from the first day (D96)
    private Integer unlockDay;

    @Column(nullable = false)
    private Integer position;

    protected EventActivity() {
        // for JPA
    }

    EventActivity(GameEvent event, String title, Integer minutes, ActivityFrequency frequency,
            Integer unlockDay, Integer position) {
        this.event = event;
        this.title = title;
        this.minutes = minutes;
        this.frequency = frequency;
        this.unlockDay = unlockDay;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public ActivityFrequency getFrequency() {
        return frequency;
    }

    public Integer getUnlockDay() {
        return unlockDay;
    }

    public Integer getPosition() {
        return position;
    }
}
