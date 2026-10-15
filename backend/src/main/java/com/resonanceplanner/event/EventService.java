package com.resonanceplanner.event;

import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.event.dto.EventResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The public reading of the events (D49, D113): every edition with its dates, computed by {@link EventCalendar}.
 * The entities become DTOs inside the transaction, because their collections load lazily.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    /** The longest range of a request, the same as the calendar. */
    public static final int MAX_RANGE_DAYS = 62;

    private static final String EVENT_NOT_FOUND = "Evento non trovato.";

    private final GameEventRepository gameEventRepository;
    private final EventCalendar calendar;
    private final Today today;

    public EventService(GameEventRepository gameEventRepository, EventCalendar calendar, Today today) {
        this.gameEventRepository = gameEventRepository;
        this.calendar = calendar;
        this.today = today;
    }

    /** One item for every edition with active days between from and to, in order of start, then of name. */
    public List<EventResponse> between(LocalDate from, LocalDate to) {
        requireValidRange(from, to);
        List<EventResponse> editions = new ArrayList<>();
        for (GameEvent event : gameEventRepository.findAllByOrderByIdAsc()) {
            for (Edition edition : calendar.between(EventSchedule.of(event), from, to)) {
                editions.add(EventResponse.from(event, edition));
            }
        }
        editions.sort(Comparator.comparing(EventResponse::startDate)
                .thenComparing(EventResponse::name)
                .thenComparing(EventResponse::id));
        return editions;
    }

    /** The edition in progress today in Italy (D41), otherwise the next one. */
    public EventResponse get(Long id) {
        GameEvent event = gameEventRepository.findById(id).orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
        return EventResponse.from(event, calendar.currentOrNext(EventSchedule.of(event), today.date()));
    }

    // Dates far from the limits of LocalDate, so the calculation of the editions can never overflow
    private static void requireValidRange(LocalDate from, LocalDate to) {
        if (!EventRules.isInWindow(from)) {
            throw new FieldValidationException("from", EventRules.DATE_WINDOW);
        }
        if (!EventRules.isInWindow(to)) {
            throw new FieldValidationException("to", EventRules.DATE_WINDOW);
        }
        if (to.isBefore(from)) {
            throw new FieldValidationException("to", "La data di fine non può venire prima di quella di inizio.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new FieldValidationException("to", "L'intervallo può coprire al massimo " + MAX_RANGE_DAYS + " giorni.");
        }
    }
}
