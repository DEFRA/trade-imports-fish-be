package uk.gov.defra.trade.imports.fish.be.controller.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.gov.defra.trade.imports.fish.be.domain.ExportRegion;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidExportDetails.Validator.class)
public @interface ValidExportDetails {
    String message() default "Country of export and CHED reference do not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidExportDetails, PatchNotificationRequest.NotificationFields> {
        @Override
        public boolean isValid(PatchNotificationRequest.NotificationFields fields,
            ConstraintValidatorContext context) {
            if (fields == null || fields.countryOfExport() == null) {
                return fields == null || fields.ched() == null;
            }

            boolean chedRequired = ExportRegion.fromCountryOfExport(fields.countryOfExport())
                == ExportRegion.REST_OF_WORLD;
            return chedRequired == (fields.ched() != null);
        }
    }
}
