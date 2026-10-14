package com.resonanceplanner.common.error;

import java.util.Map;

/**
 * A value is wrong because of a rule that only the service knows, for example a play time that is not among the
 * options of the game rules (D47). It answers 400 like the Bean Validation errors, with the name of the field, so the
 * frontend shows the message under the right input.
 */
public class FieldValidationException extends ApiException {

    public FieldValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_ERROR, message, Map.of(field, message));
    }

    /** Several fields are wrong at once: the user sees every message together instead of fixing one at a time. */
    public FieldValidationException(Map<String, String> fields) {
        super(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), fields);
    }
}
