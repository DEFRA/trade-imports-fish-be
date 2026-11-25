package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Species(
    String speciesCode,
    String commonName,
    String scientificName,
    Double netWeightKg,
    Double netFisheryProductWeightKg
) {}
