package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PatchNotificationRequest(
    @NotBlank(message = "Organisation ID is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String organisationId,
    @Valid @NotNull(message = "Notification content is required") NotificationFields notification
) {}