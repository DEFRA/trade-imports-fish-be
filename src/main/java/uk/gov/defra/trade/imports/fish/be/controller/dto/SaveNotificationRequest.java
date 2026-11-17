package uk.gov.defra.trade.imports.fish.be.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uk.gov.defra.trade.imports.fish.be.domain.TransportMethod;

public record SaveNotificationRequest(
    @NotBlank(message = "Organisation ID is required") String organisationId,
    @NotNull(message = "Transport method is required") TransportMethod transportMethod
) {}
