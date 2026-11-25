package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Not a record: Jackson treats an omitted record component like an explicit null, but omitted sections must be
 * allowed while explicit nulls are rejected.
 */
@AtLeastOneSection
@ValidExportDetails
public final class NotificationFields {
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

    @Valid
    @JsonSetter(nulls = Nulls.FAIL)
    @NotEmptyIfPresent(message = "Commodities list must not be empty")
    private List<CommodityFields> commodities;

    // Used by Jackson via reflection during JSON deserialization
    @SuppressWarnings("unused")
    private NotificationFields() {}

    public NotificationFields(SaveNotificationRequest.TransportFields transport, ArrivalFields arrival) {
        this(transport, arrival, null, null, null);
    }

    public NotificationFields(SaveNotificationRequest.TransportFields transport, ArrivalFields arrival,
        String countryOfExport, ChedFields ched) {
        this(transport, arrival, countryOfExport, ched, null);
    }

    public NotificationFields(SaveNotificationRequest.TransportFields transport, ArrivalFields arrival,
        String countryOfExport, ChedFields ched, List<CommodityFields> commodities) {
        this.transport = transport;
        this.arrival = arrival;
        this.countryOfExport = countryOfExport;
        this.ched = ched;
        this.commodities = commodities;
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

    public List<CommodityFields> commodities() {
        return commodities;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setTransport(SaveNotificationRequest.TransportFields transport) {
        this.transport = transport;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setArrival(ArrivalFields arrival) {
        this.arrival = arrival;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setCountryOfExport(String countryOfExport) {
        this.countryOfExport = countryOfExport;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setChed(ChedFields ched) {
        this.ched = ched;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setCommodities(List<CommodityFields> commodities) {
        this.commodities = commodities;
    }
}
