package uk.gov.defra.trade.imports.fish.be.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private TransportMethod transportMethod;

    /**
     * Transport method from the original creation request. Never changed by updates, so idempotent
     * creation replays are validated and answered against the original request.
     */
    @JsonIgnore
    private TransportMethod creationTransportMethod;

    public Notification(String id, String reference, NotificationStatus status, String organisationId,
        Instant createdAt, Instant updatedAt, TransportMethod transportMethod) {
        this.id = id;
        this.reference = reference;
        this.status = status;
        this.organisationId = organisationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.transportMethod = transportMethod;
        this.creationTransportMethod = transportMethod;
    }
}
