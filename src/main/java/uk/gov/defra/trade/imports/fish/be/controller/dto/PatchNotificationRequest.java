package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PatchNotificationRequest(
    @NotBlank(message = "Organisation ID is required")
    @JsonDeserialize(using = StrictStringDeserializer.class) String organisationId,
    @Valid @NotNull(message = "Notification content is required") NotificationFields notification
) {
    /**
     * Not a record: Jackson treats an omitted record component like an explicit null, but omitted sections must be
     * allowed while explicit nulls are rejected.
     */
    @AtLeastOneSection
    @ValidExportDetails
    public static final class NotificationFields {
        @Valid
        @JsonSetter(nulls = Nulls.FAIL)
        private SaveNotificationRequest.TransportFields transport;

        @Valid
        @JsonSetter(nulls = Nulls.FAIL)
        private ArrivalFields arrival;

        @JsonSetter(nulls = Nulls.FAIL)
        @JsonDeserialize(using = StrictStringDeserializer.class)
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country of export must be a two-letter upper-case country code")
        private String countryOfExport;

        @Valid
        @JsonSetter(nulls = Nulls.FAIL)
        private ChedFields ched;

        private NotificationFields() {}

        public NotificationFields(SaveNotificationRequest.TransportFields transport, ArrivalFields arrival) {
            this(transport, arrival, null, null);
        }

        public NotificationFields(SaveNotificationRequest.TransportFields transport, ArrivalFields arrival,
            String countryOfExport, ChedFields ched) {
            this.transport = transport;
            this.arrival = arrival;
            this.countryOfExport = countryOfExport;
            this.ched = ched;
        }

        public SaveNotificationRequest.TransportFields transport() {
            return transport;
        }

        public ArrivalFields arrival() {
            return arrival;
        }

        public String countryOfExport() {
            return countryOfExport;
        }

        public ChedFields ched() {
            return ched;
        }
    }

    public record ChedFields(
        @NotBlank(message = "CHED reference is required")
        @Pattern(regexp = "^CHEDP\\.GB\\.\\d{4}\\.\\d{7}$",
            message = "CHED reference must be in the format CHEDP.GB.YYYY.NNNNNNN")
        @JsonDeserialize(using = StrictStringDeserializer.class) String reference
    ) {}

    @ExpectedArrival
    public record ArrivalFields(
        @NotBlank(message = "Port of entry is required")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "Port of entry must be an upper-case port code")
        @JsonDeserialize(using = StrictStringDeserializer.class) String portOfEntry,
        @NotBlank(message = "Expected arrival date is required")
        @CalendarDate(message = "Expected arrival date must be a real date in the format YYYY-MM-DD")
        @JsonDeserialize(using = StrictStringDeserializer.class) String expectedArrivalDate,
        @NotBlank(message = "Expected arrival time is required")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "Expected arrival time must be a 24-hour time in the format HH:mm")
        @JsonDeserialize(using = StrictStringDeserializer.class) String expectedArrivalTime
    ) {}
}
