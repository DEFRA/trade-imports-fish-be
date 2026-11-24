package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationContent(
    Transport transport,
    Arrival arrival,
    String landingType,
    String countryOfExport,
    String exportRegion,
    Map<String, Object> ched,
    List<Map<String, Object>> commodities,
    Map<String, Object> importer,
    Map<String, Object> agent,
    String responsibleParty,
    Map<String, Object> exporter,
    List<Map<String, Object>> documents,
    Map<String, Object> declaration
) {
    public NotificationContent(Transport transport) {
        this(transport, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Transport(
        TransportMode mode,
        String vesselName,
        String imoNumber,
        String flightNumber,
        String vehicleRegistration,
        List<String> containerNumbers,
        String transportDocumentReference
    ) {
        public Transport(TransportMode mode) {
            this(mode, null, null, null, null, null, null);
        }
    }
}
