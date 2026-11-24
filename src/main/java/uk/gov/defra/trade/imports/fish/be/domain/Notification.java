package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
public class Notification {
    @Id
    private String id;

    @Indexed(unique = true)
    private String reference;

    private NotificationStatus status;
    private String organisationId;
    private Instant createdAt;
    private Instant updatedAt;
    private NotificationContent notification;
    private String importerOrganisationId;
    private Map<String, Object> createdBy;
    private Integer concurrencyToken;
    private Instant submittedAt;
    private Instant receivedAt;
    private List<Map<String, Object>> fieldProvenance;

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private CreationRequest creationRequest;

    public record CreationRequest(String organisationId, NotificationContent notification) {}

    public Notification(String id, String reference, NotificationStatus status, String organisationId,
        Instant createdAt, Instant updatedAt, TransportMode mode) {
        this.id = id;
        this.reference = reference;
        this.status = status;
        this.organisationId = organisationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.notification = new NotificationContent(new NotificationContent.Transport(mode));
        this.creationRequest = new CreationRequest(organisationId, this.notification);
    }
}
