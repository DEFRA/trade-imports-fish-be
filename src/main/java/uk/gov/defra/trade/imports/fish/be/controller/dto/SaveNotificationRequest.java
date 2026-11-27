package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uk.gov.defra.trade.imports.fish.be.domain.TransportMode;

public record SaveNotificationRequest(
    @NotBlank(message = "Organisation ID is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String organisationId,
    @Valid @NotNull(message = "Notification content is required") NotificationFields notification
) {
    public record NotificationFields(
        @Valid @NotNull(message = "Transport is required") TransportFields transport
    ) {}

    public record TransportFields(
        @NotNull(message = "Transport mode is required") TransportMode mode
    ) {}
}
