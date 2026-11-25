package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpeciesFields(
    @NotBlank(message = "Species code is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String speciesCode,
    @NotBlank(message = "Species common name is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String commonName,
    @NotBlank(message = "Species scientific name is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String scientificName,
    @NotNull(message = "Net weight is required")
    @DecimalMin(value = "0", message = "Net weight must be greater than 0", inclusive = false)
    Double netWeightKg,
    @DecimalMin(value = "0", message = "Net fishery product weight must be greater than 0")
    Double netFisheryProductWeightKg
) {}
