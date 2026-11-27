package uk.gov.defra.trade.imports.fish.be.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationContent;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
    String id,
    String reference,
    String organisationId,
    NotificationStatus status,
    Instant createdAt,
    Instant updatedAt,
    NotificationContent notification,
    String importerOrganisationId,
    Map<String, Object> createdBy,
    Integer concurrencyToken,
    Instant submittedAt,
    Instant receivedAt,
    List<Map<String, Object>> fieldProvenance
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getReference(),
            notification.getOrganisationId(), notification.getStatus(), notification.getCreatedAt(),
            notification.getUpdatedAt(), notification.getNotification(), notification.getImporterOrganisationId(),
            notification.getCreatedBy(), notification.getConcurrencyToken(), notification.getSubmittedAt(),
            notification.getReceivedAt(), notification.getFieldProvenance());
    }
}
