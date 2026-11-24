package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ExpectedArrival
public record ArrivalFields(
    @NotBlank(message = "Port of entry is required")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Port of entry must be an upper-case port code")
    @JsonDeserialize(using = StrictStringDeserializer.class) String portOfEntry,
    @NotBlank(message = "Expected arrival date is required")
    @CalendarDate(message = "Expected arrival date must be a real date in the format YYYY-MM-DD")
    @JsonDeserialize(using = StrictStringDeserializer.class) String expectedArrivalDate,
    @NotBlank(message = "Expected arrival time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
        message = "Expected arrival time must be a 24-hour time in the format HH:mm")
    @JsonDeserialize(using = StrictStringDeserializer.class) String expectedArrivalTime
) {}
