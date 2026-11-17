package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransportMethod {
    SHIP_OR_VESSEL("ship-or-vessel"),
    AIR_FREIGHT("air-freight"),
    ROAD_VEHICLE("road-vehicle");

    private final String value;

    TransportMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
