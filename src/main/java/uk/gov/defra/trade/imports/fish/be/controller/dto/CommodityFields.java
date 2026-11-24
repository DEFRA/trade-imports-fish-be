package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CommodityFields(
    @NotBlank(message = "Commodity code is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String commodityCode,
    @NotBlank(message = "Commodity description is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String description,
    @jakarta.validation.Valid
    @NotEmpty(message = "At least one species is required")
    List<SpeciesFields> species,
    @NotNull(message = "Total net weight is required")
    @DecimalMin(value = "0", message = "Total net weight must be at least 0")
    Double totalNetWeightKg,
    @DecimalMin(value = "0", message = "Total net fishery product weight must be at least 0")
    Double totalNetFisheryProductWeightKg
) {}
