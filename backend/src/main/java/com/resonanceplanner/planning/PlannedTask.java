package com.resonanceplanner.planning;

import com.resonanceplanner.catalog.FarmingSource;
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
import java.time.Instant;
import java.time.LocalDate;

/**
 * A task of the calendar (D35): farming runs, an activity of an event or something written by the user, all in one
 * table. User, proposal, event and character are plain identifiers: the foreign keys guarantee that they exist.
 * The columns that never change after the insert are not updatable, so writing a task back can never restore a value
 * that the database changed in the meantime (the event is set to null when the admin deletes it).
 */
@Entity
@Table(name = "planned_task")
public class PlannedTask {

    public static final int MAX_TITLE_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    // Only the tasks of the agent belong to a proposal (D22)
    @Column(updatable = false)
    private Long proposalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TaskOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TaskKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false)
    private LocalDate taskDate;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    private String notes;

    @Column(nullable = false)
    private int minutes;

    // FARMING only: where the runs are done and how many
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farming_source_id", updatable = false)
    private FarmingSource farmingSource;

    @Column(updatable = false)
    private Integer runs;

    // EVENT only
    @Column(updatable = false)
    private Long eventId;

    @Column(updatable = false)
    private String characterId;

    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected PlannedTask() {
        // for JPA
    }

    /** A task written by the user (D24): always CUSTOM, outside any proposal, and already in the calendar. */
    public static PlannedTask manual(Long userId, String title, LocalDate taskDate, int minutes, String notes,
            Instant now) {
        PlannedTask task = new PlannedTask();
        task.userId = userId;
        task.origin = TaskOrigin.MANUAL;
        task.kind = TaskKind.CUSTOM;
        task.status = TaskStatus.CONFIRMED;
        task.createdAt = now;
        task.changeManual(title, taskDate, minutes, notes);
        return task;
    }

    /** The fields of the form of a manual task; the status stays as it is, also when the task is done. */
    public void changeManual(String title, LocalDate taskDate, int minutes, String notes) {
        this.title = title;
        this.taskDate = taskDate;
        this.minutes = minutes;
        this.notes = notes;
    }

    public void markDone(Instant now) {
        this.status = TaskStatus.DONE;
        this.completedAt = now;
    }

    public boolean isManual() {
        return origin == TaskOrigin.MANUAL;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public TaskOrigin getOrigin() {
        return origin;
    }

    public TaskKind getKind() {
        return kind;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public int getMinutes() {
        return minutes;
    }

    public FarmingSource getFarmingSource() {
        return farmingSource;
    }

    public Integer getRuns() {
        return runs;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getCharacterId() {
        return characterId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
