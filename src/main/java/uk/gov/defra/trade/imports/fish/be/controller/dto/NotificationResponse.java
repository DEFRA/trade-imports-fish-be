package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationAggregate;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
    String id,
    String reference,
    String organisationId,
    NotificationStatus status,
    Instant createdAt,
    Instant updatedAt,
    Notification notification,
    String importerOrganisationId,
    Map<String, Object> createdBy,
    Integer concurrencyToken,
    Instant submittedAt,
    Instant receivedAt,
    List<Map<String, Object>> fieldProvenance
) {
    public static NotificationResponse from(NotificationAggregate notificationRecord) {
        return new NotificationResponse(notificationRecord.getId(), notificationRecord.getReference(), notificationRecord.getOrganisationId(),
            notificationRecord.getStatus(), notificationRecord.getCreatedAt(), notificationRecord.getUpdatedAt(), notificationRecord.getNotification(),
            notificationRecord.getImporterOrganisationId(), notificationRecord.getCreatedBy(), notificationRecord.getConcurrencyToken(),
            notificationRecord.getSubmittedAt(), notificationRecord.getReceivedAt(), notificationRecord.getFieldProvenance());
    }
}
