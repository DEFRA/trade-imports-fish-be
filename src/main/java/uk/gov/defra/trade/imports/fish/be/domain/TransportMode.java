package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransportMode {
    VESSEL,
    AIR_FREIGHT,
    ROAD_VEHICLE;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TransportMode fromValue(String value) {
        // Leave missing values to Bean Validation so errors retain the nested field path.
        return value == null ? null : valueOf(value);
    }
}
