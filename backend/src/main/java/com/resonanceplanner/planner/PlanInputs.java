package com.resonanceplanner.planner;

import com.resonanceplanner.event.EventService;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.event.dto.EventResponse.Activity;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.planner.PlannerInput.CharacterGoal;
import com.resonanceplanner.roster.OwnedCharacterRepository;
import com.resonanceplanner.roster.Progress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The fingerprint of the data a plan is made from (D23, D125): the roster with its goals, and the events of the days
 * of the plan. A proposal keeps it, and the calendar compares it with the one of today's data. The inventory and the
 * tasks done are left out on purpose: they change at every "Segna come fatta".
 */
@Component
public class PlanInputs {

    private static final String LINE = "\n";

    private final OwnedCharacterRepository ownedCharacterRepository;
    private final EventService eventService;

    public PlanInputs(OwnedCharacterRepository ownedCharacterRepository, EventService eventService) {
        this.ownedCharacterRepository = ownedCharacterRepository;
        this.eventService = eventService;
    }

    /** The fingerprint of today's data for the days of a plan, both included. */
    @Transactional(readOnly = true)
    public String fingerprint(Long userId, LocalDate firstDay, LocalDate lastDay) {
        List<CharacterGoal> roster = ownedCharacterRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(owned -> new CharacterGoal(owned.getCharacterId(), owned.getProgress()))
                .toList();
        return fingerprintOf(roster, eventService.between(firstDay, lastDay));
    }

    /**
     * SHA-256 of a text that writes every field: the same data always give the same 64 hexadecimal digits. Every
     * field is written with its length first, so no text of the data (an event name, a title) can look like two
     * fields or two lines.
     */
    static String fingerprintOf(List<CharacterGoal> roster, List<EventResponse> events) {
        List<String> lines = new ArrayList<>();
        roster.stream().map(PlanInputs::lineOf).sorted().forEach(lines::add);
        events.stream().map(PlanInputs::lineOf).sorted().forEach(lines::add);
        return sha256(String.join(LINE, lines));
    }

    private static String lineOf(CharacterGoal goal) {
        Progress progress = goal.progress();
        return join("character", goal.characterId(), progress.currentLevel(), progress.currentRank(),
                progress.targetLevel(), progress.targetRank());
    }

    private static String lineOf(EventResponse event) {
        List<Object> fields = new ArrayList<>(List.of("event", event.id(), event.name(), event.startDate(),
                event.endDate(), event.activeDates()));
        for (Activity activity : event.activities()) {
            fields.add(join("activity", activity.title(), activity.minutes(), activity.frequency(),
                    activity.unlockDay()));
        }
        Effect effect = event.effect();
        if (effect != null) {
            // 2 and 2.00 are the same multiplier
            fields.add(join("effect", effect.type(), effect.sourceType(),
                    effect.multiplier().stripTrailingZeros().toPlainString(), effect.dailyLimit()));
        }
        return join(fields.toArray());
    }

    // "5:jinhsi" for a text of 5 characters, "-" for a missing value: "null" written as a text stays different
    private static String join(Object... fields) {
        StringBuilder text = new StringBuilder();
        for (Object field : fields) {
            if (field == null) {
                text.append('-');
            } else {
                String value = field.toString();
                text.append(value.length()).append(':').append(value);
            }
        }
        return text.toString();
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Every Java runtime has SHA-256
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
