package com.resonanceplanner.agent.client;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * The calls to the model of one user in one day (D129): the day is in UTC, like the free limit of OpenRouter (D126).
 * It is only read: the count grows with one upsert of {@link ModelCallDayRepository}.
 */
@Entity
@Table(name = "agent_model_calls")
public class ModelCallDay {

    @EmbeddedId
    private Key id;

    @Column(nullable = false)
    private int calls;

    protected ModelCallDay() {
        // for JPA
    }

    /** The day and the user of a row. */
    @Embeddable
    public record Key(LocalDate callDate, Long userId) {}
}
