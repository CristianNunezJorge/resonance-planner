package com.resonanceplanner.event;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A timed game event. Demo events are RECURRING, with dates computed from the anchor date (D29);
 * events created by the admin can also be FIXED, with real dates (D14).
 */
@Entity
@Table(name = "game_event")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifier of the JSON file for demo events; empty for events created by the admin
    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer startOffsetDays;
    private Integer durationDays;
    private Integer repeatEveryDays;

    @Embedded
    private EventEffect effect;

    // No rows = active every day (D96)
    @ElementCollection
    @CollectionTable(name = "event_active_weekday", joinColumns = @JoinColumn(name = "event_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> activeWeekdays = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "event_reward", joinColumns = @JoinColumn(name = "event_id"))
    @OrderColumn(name = "position")
    @Column(name = "reward")
    private List<String> rewards = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<EventActivity> activities = new ArrayList<>();

    protected GameEvent() {
        // for JPA
    }

    private GameEvent(String code, String name, EventType type, String description, ScheduleType scheduleType) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.description = description;
        this.scheduleType = scheduleType;
    }

    /** A demo event that repeats every repeatEveryDays, starting startOffsetDays after the anchor date (D29). */
    public static GameEvent recurring(String code, String name, EventType type, String description,
            int startOffsetDays, int durationDays, int repeatEveryDays) {
        GameEvent event = new GameEvent(code, name, type, description, ScheduleType.RECURRING);
        event.startOffsetDays = startOffsetDays;
        event.durationDays = durationDays;
        event.repeatEveryDays = repeatEveryDays;
        return event;
    }

    /**
     * An event created by the admin (D14): no code, because it is not in events.json. The schedule must be set with
     * {@link #scheduleFixed} or {@link #scheduleRecurring} before it is saved.
     */
    public static GameEvent custom(String name, EventType type, String description) {
        return new GameEvent(null, name, type, description, ScheduleType.FIXED);
    }

    public void changeDetails(String name, EventType type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    /** Real dates, last day included; the values of a recurring event are cleared, as the database requires. */
    public void scheduleFixed(LocalDate startDate, LocalDate endDate) {
        this.scheduleType = ScheduleType.FIXED;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startOffsetDays = null;
        this.durationDays = null;
        this.repeatEveryDays = null;
    }

    /** Editions computed from the anchor date (D29); the real dates are cleared, as the database requires. */
    public void scheduleRecurring(int startOffsetDays, int durationDays, int repeatEveryDays) {
        this.scheduleType = ScheduleType.RECURRING;
        this.startDate = null;
        this.endDate = null;
        this.startOffsetDays = startOffsetDays;
        this.durationDays = durationDays;
        this.repeatEveryDays = repeatEveryDays;
    }

    public void setEffect(EventEffect effect) {
        this.effect = effect;
    }

    // No days or all seven mean the same, every day (D96): both are stored as no rows
    public void setActiveWeekdays(Set<DayOfWeek> weekdays) {
        boolean everyDay = weekdays.isEmpty() || weekdays.size() == DayOfWeek.values().length;
        this.activeWeekdays = everyDay ? new HashSet<>() : EnumSet.copyOf(weekdays);
    }

    /** Removes every activity; the database deletes them at the next flush (orphan removal). */
    public void removeActivities() {
        activities.clear();
    }

    public void setRewards(List<String> rewards) {
        this.rewards = new ArrayList<>(rewards);
    }

    /** Adds an activity at the end of the list; its position follows the order of addition. */
    public void addActivity(String title, int minutes, ActivityFrequency frequency, Integer unlockDay) {
        activities.add(new EventActivity(this, title, minutes, frequency, unlockDay, activities.size()));
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public EventType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getStartOffsetDays() {
        return startOffsetDays;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public Integer getRepeatEveryDays() {
        return repeatEveryDays;
    }

    public EventEffect getEffect() {
        return effect;
    }

    public Set<DayOfWeek> getActiveWeekdays() {
        return Collections.unmodifiableSet(activeWeekdays);
    }

    public List<String> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    public List<EventActivity> getActivities() {
        return Collections.unmodifiableList(activities);
    }
}
