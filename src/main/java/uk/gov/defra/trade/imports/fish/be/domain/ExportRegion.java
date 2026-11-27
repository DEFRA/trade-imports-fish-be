package uk.gov.defra.trade.imports.fish.be.domain;

import java.util.Set;

public enum ExportRegion {
    EU,
    REST_OF_WORLD;

    private static final Set<String> EU_27_COUNTRY_CODES = Set.of(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE",
        "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );

    public static ExportRegion fromCountryOfExport(String countryCode) {
        return EU_27_COUNTRY_CODES.contains(countryCode) ? EU : REST_OF_WORLD;
    }
}
