package uk.gov.defra.trade.imports.fish.be.controller.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * Validates that a collection is not empty if it is present (not null).
 * Allows null values but rejects empty collections.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotEmptyIfPresent.Validator.class)
public @interface NotEmptyIfPresent {
    String message() default "Collection must not be empty when provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<NotEmptyIfPresent, Collection<?>> {
        @Override
        public boolean isValid(Collection<?> value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            return !value.isEmpty();
        }
    }
}
