package uk.gov.defra.trade.imports.fish.be.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
