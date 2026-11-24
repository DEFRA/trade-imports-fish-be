package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Commodity(
    String commodityCode,
    String description,
    List<Species> species,
    Double totalNetWeightKg,
    Double totalNetFisheryProductWeightKg
) {}
