package uk.gov.defra.trade.imports.fish.be.controller.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.defra.trade.imports.fish.be.domain.Arrival;

/**
 * The expected arrival, read as UK local time, must exist and must not be in the past. Format errors are left to
 * the field constraints.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExpectedArrival.Validator.class)
public @interface ExpectedArrival {
    String message() default "Expected arrival is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ExpectedArrival, PatchNotificationRequest.ArrivalFields> {
        private final Clock clock;

        public Validator() {
            this(Clock.systemUTC());
        }

        @Autowired(required = false)
        public Validator(Clock clock) {
            this.clock = clock;
        }

        @Override
        public boolean isValid(PatchNotificationRequest.ArrivalFields arrival, ConstraintValidatorContext context) {
            LocalDateTime local;
            try {
                local = LocalDate.parse(arrival.expectedArrivalDate())
                    .atTime(LocalTime.parse(arrival.expectedArrivalTime()));
            } catch (DateTimeException | NullPointerException exception) {
                return true;
            }
            if (Arrival.UK.getRules().getValidOffsets(local).isEmpty()) {
                return reject(context, "Expected arrival time does not exist on this date because the UK clocks go forward");
            }
            if (local.atZone(Arrival.UK).toInstant().isBefore(clock.instant())) {
                return reject(context, "Expected arrival must not be in the past");
            }
            return true;
        }

        private static boolean reject(ConstraintValidatorContext context, String message) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("expectedArrivalTime").addConstraintViolation();
            return false;
        }
    }
}
