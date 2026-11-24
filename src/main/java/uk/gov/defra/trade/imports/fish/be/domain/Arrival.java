package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Arrival(
    String portOfEntry,
    String borderControlPost,
    String expectedArrivalDate,
    String expectedArrivalTime,
    Instant expectedArrivalAt
) {
    public static final ZoneId UK = ZoneId.of("Europe/London");

    /** An arrival as entered by the importer: the expected date and time are UK local time. */
    public static Arrival entered(String portOfEntry, String expectedArrivalDate, String expectedArrivalTime) {
        Instant expectedArrivalAt = LocalDate.parse(expectedArrivalDate)
            .atTime(LocalTime.parse(expectedArrivalTime)).atZone(UK).toInstant();
        return new Arrival(portOfEntry, null, expectedArrivalDate, expectedArrivalTime, expectedArrivalAt);
    }
}
