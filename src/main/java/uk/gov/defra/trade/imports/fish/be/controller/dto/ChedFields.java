package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChedFields(
    @NotBlank(message = "CHED reference is required")
    @Pattern(regexp = "^CHEDP\\.GB\\.\\d{4}\\.\\d{7}$",
        message = "CHED reference must be in the format CHEDP.GB.YYYY.NNNNNNN")
    @JsonDeserialize(using = StrictStringDeserializer.class) String reference
) {}
