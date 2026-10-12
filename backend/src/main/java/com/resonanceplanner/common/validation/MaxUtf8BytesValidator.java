package com.resonanceplanner.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/** Null counts as valid, like the other constraints: {@code @NotNull} is the one that rejects it. */
public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, CharSequence> {

    private int maxBytes;

    @Override
    public void initialize(MaxUtf8Bytes constraint) {
        this.maxBytes = constraint.value();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || value.toString().getBytes(StandardCharsets.UTF_8).length <= maxBytes;
    }
}
