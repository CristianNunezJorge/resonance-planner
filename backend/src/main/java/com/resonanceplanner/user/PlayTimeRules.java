package com.resonanceplanner.user;

import com.resonanceplanner.catalog.GameRules;
import com.resonanceplanner.common.error.FieldValidationException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The options of the daily play time, which are in game-rules.json (30, 60 or 120 minutes). Used by the profile and by
 * the guided setup, so the rule and its message exist once.
 */
@Component
public class PlayTimeRules {

    private final List<Integer> options;

    public PlayTimeRules(GameRules gameRules) {
        this.options = gameRules.playTimeOptionsMinutes();
    }

    /** The message to show under the input if the value is not an option; empty if it is. */
    public Optional<String> problem(int minutes) {
        if (options.contains(minutes)) {
            return Optional.empty();
        }
        return Optional.of("Scegli un tempo di gioco tra " + describe(options) + " minuti.");
    }

    public void requireValid(int minutes) {
        problem(minutes).ifPresent(message -> {
            throw new FieldValidationException("playTimeMinutes", message);
        });
    }

    // [30, 60, 120] becomes "30, 60 o 120"
    private static String describe(List<Integer> options) {
        String last = String.valueOf(options.getLast());
        if (options.size() == 1) {
            return last;
        }
        String others = options.subList(0, options.size() - 1).stream().map(String::valueOf).collect(Collectors.joining(", "));
        return others + " o " + last;
    }
}
