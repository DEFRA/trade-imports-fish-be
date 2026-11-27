package uk.gov.defra.trade.imports.fish.be.controller.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** A real calendar date written exactly as YYYY-MM-DD. Null is left to {@code @NotBlank}. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CalendarDate.Validator.class)
public @interface CalendarDate {
    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<CalendarDate, String> {
        private static final Pattern FORMAT = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            if (!FORMAT.matcher(value).matches()) {
                return false;
            }
            try {
                LocalDate.parse(value);
                return true;
            } catch (DateTimeParseException exception) {
                return false;
            }
        }
    }
}
