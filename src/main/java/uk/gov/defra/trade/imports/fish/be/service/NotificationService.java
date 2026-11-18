package uk.gov.defra.trade.imports.fish.be.service;

import com.mongodb.ReadPreference;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.fish.be.controller.dto.SaveNotificationRequest;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationStatus;
import uk.gov.defra.trade.imports.fish.be.domain.repository.NotificationRepository;
import uk.gov.defra.trade.imports.fish.be.exceptions.ConflictException;
import uk.gov.defra.trade.imports.fish.be.exceptions.NotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final NotificationRepository repository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final MongoTemplate mongoTemplate;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public record CreationResult(Notification notification, boolean created) {}

    public CreationResult create(UUID key, SaveNotificationRequest request) {
        String id = key.toString();
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        Instant createdAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            Notification notification = new Notification(id, referenceNumberGenerator.generate(),
                NotificationStatus.DRAFT, request.organisationId(), createdAt, createdAt,
                request.transportMethod());
            try {
                Notification saved = repository.insert(notification);
                meterRegistry.counter("notification_created").increment();
                log.info("Created draft notification with id: {}", id);
                return new CreationResult(saved, true);
            } catch (DuplicateKeyException exception) {
                var concurrent = repository.findById(id);
                if (concurrent.isPresent()) {
                    return replay(concurrent.get(), request);
                }
                if (!repository.existsByReference(notification.getReference())) {
                    throw exception;
                }
                log.warn("Notification reference collision; retrying generation");
            }
        }
        throw new IllegalStateException("Unable to allocate a unique notification reference");
    }

    public Notification findById(UUID id) {
        return repository.findById(id.toString())
            .orElseThrow(() -> new NotFoundException("Notification not found with id: " + id));
    }

    public Notification update(UUID id, SaveNotificationRequest request) {
        Query query = Query.query(Criteria.where("_id").is(id.toString())
            .and("organisationId").is(request.organisationId())
            .and("status").is(NotificationStatus.DRAFT))
            .withReadPreference(ReadPreference.primary());
        Update update = new Update()
            .set("transportMethod", request.transportMethod())
            .set("updatedAt", clock.instant().truncatedTo(ChronoUnit.MILLIS));

        Notification updated = mongoTemplate.findAndModify(query, update,
            FindAndModifyOptions.options().returnNew(true), Notification.class);
        if (updated == null) {
            findById(id);
            throw new ConflictException("Only a draft belonging to the same organisation can be updated");
        }
        meterRegistry.counter("notification_updated").increment();
        log.info("Updated draft notification with id: {}", id);
        return updated;
    }

    private CreationResult replay(Notification notification, SaveNotificationRequest request) {
        if (notification.getStatus() != NotificationStatus.DRAFT
            || !notification.getOrganisationId().equals(request.organisationId())
            || notification.getCreationTransportMethod() != request.transportMethod()) {
            throw new ConflictException("Idempotency key has already been used with a different request");
        }
        log.info("Replayed draft creation with id: {}", notification.getId());
        Notification original = new Notification(notification.getId(), notification.getReference(),
            notification.getStatus(), notification.getOrganisationId(), notification.getCreatedAt(),
            notification.getCreatedAt(), notification.getCreationTransportMethod());
        return new CreationResult(original, false);
    }
}
