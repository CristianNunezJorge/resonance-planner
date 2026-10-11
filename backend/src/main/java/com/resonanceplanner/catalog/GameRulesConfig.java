package com.resonanceplanner.catalog;

import com.resonanceplanner.common.data.DataFileReader;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.type.TypeReference;

/** Reads game-rules.json once at startup; the application stops if the file is missing or invalid (D33). */
@Configuration(proxyBeanMethods = false)
public class GameRulesConfig {

    private static final String RULES_FILE = "game-rules.json";

    @Bean
    public GameRules gameRules(DataFileReader dataFileReader) {
        GameRules rules = dataFileReader.read(RULES_FILE, new TypeReference<GameRules>() {});
        checkValid(rules);
        return rules;
    }

    private static void checkValid(GameRules rules) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<GameRules>> violations = factory.getValidator().validate(rules);
            if (!violations.isEmpty()) {
                String problems = violations.stream()
                        .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                        .collect(Collectors.joining(", "));
                throw new IllegalStateException(RULES_FILE + " is not valid: " + problems);
            }
        }
    }
}
