package uk.gov.defra.trade.imports.fish.be.controller.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A notification patch must change at least one supported section. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneSection.Validator.class)
public @interface AtLeastOneSection {
    String message() default "Transport or arrival is required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AtLeastOneSection, PatchNotificationRequest.NotificationFields> {
        @Override
        public boolean isValid(PatchNotificationRequest.NotificationFields fields, ConstraintValidatorContext context) {
            return fields == null || fields.transport() != null || fields.arrival() != null;
        }
    }
}
