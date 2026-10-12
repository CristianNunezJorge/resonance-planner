package com.resonanceplanner.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The text takes at most {@code value} bytes in UTF-8. {@code @Size} counts characters, but BCrypt
 * accepts at most 72 bytes and an emoji is 4 bytes: with only {@code @Size} a long password would end in a 500.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
        ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "Il testo è troppo lungo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
