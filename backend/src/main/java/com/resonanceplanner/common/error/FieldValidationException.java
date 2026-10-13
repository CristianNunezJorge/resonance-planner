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
}
