package uk.gov.defra.trade.imports.fish.be.service;

import com.mongodb.ReadPreference;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import uk.gov.defra.trade.imports.fish.be.controller.dto.CommodityFields;
import uk.gov.defra.trade.imports.fish.be.controller.dto.PatchNotificationRequest;
import uk.gov.defra.trade.imports.fish.be.controller.dto.SaveNotificationRequest;
import uk.gov.defra.trade.imports.fish.be.controller.dto.SpeciesFields;
import uk.gov.defra.trade.imports.fish.be.domain.Arrival;
import uk.gov.defra.trade.imports.fish.be.domain.Commodity;
import uk.gov.defra.trade.imports.fish.be.domain.ExportRegion;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationAggregate;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationStatus;
import uk.gov.defra.trade.imports.fish.be.domain.Species;
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

    public record CreationResult(NotificationAggregate notification, boolean created) {}

    public CreationResult create(UUID key, SaveNotificationRequest request) {
        String id = key.toString();
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        Instant createdAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            NotificationAggregate notification = new NotificationAggregate(id, referenceNumberGenerator.generate(),
                NotificationStatus.DRAFT, request.organisationId(), createdAt, createdAt,
                request.notification().transport().mode());
            try {
                NotificationAggregate saved = repository.insert(notification);
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

    public NotificationAggregate findById(UUID id) {
        return repository.findById(id.toString())
            .orElseThrow(() -> new NotFoundException("Notification not found with id: " + id));
    }

    public NotificationAggregate update(UUID id, PatchNotificationRequest request) {
        Query query = Query.query(Criteria.where("_id").is(id.toString())
            .and("organisationId").is(request.organisationId())
            .and("status").is(NotificationStatus.DRAFT))
            .withReadPreference(ReadPreference.primary());
        var fields = request.notification();
        Update update = new Update();
        if (fields.transport() != null) {
            update.set("notification.transport.mode", fields.transport().mode());
        }
        if (fields.arrival() != null) {
            Arrival arrival = Arrival.entered(fields.arrival().portOfEntry(),
                fields.arrival().expectedArrivalDate(), fields.arrival().expectedArrivalTime());
            update.set("notification.arrival.portOfEntry", arrival.portOfEntry())
                .set("notification.arrival.expectedArrivalDate", arrival.expectedArrivalDate())
                .set("notification.arrival.expectedArrivalTime", arrival.expectedArrivalTime())
                .set("notification.arrival.expectedArrivalAt", arrival.expectedArrivalAt());
        }
        if (fields.countryOfExport() != null) {
            ExportRegion exportRegion = ExportRegion.fromCountryOfExport(fields.countryOfExport());
            update.set("notification.countryOfExport", fields.countryOfExport())
                .set("notification.exportRegion", exportRegion);
            if (exportRegion == ExportRegion.EU) {
                update.unset("notification.ched");
            } else {
                update.set("notification.ched.reference", fields.ched().reference());
            }
        }
        if (fields.commodities() != null) {
            List<Commodity> commodities = fields.commodities().stream()
                .map(this::toCommodityDomain)
                .toList();
            update.set("notification.commodities", commodities);
        }
        update.currentDate("updatedAt");

        NotificationAggregate updated = mongoTemplate.findAndModify(query, update,
            FindAndModifyOptions.options().returnNew(true), NotificationAggregate.class);
        if (updated == null) {
            if (!repository.existsById(id.toString())) {
                throw new NotFoundException("Notification not found with id: " + id);
            }
            throw new ConflictException("Only a draft belonging to the same organisation can be updated");
        }
        meterRegistry.counter("notification_updated").increment();
        log.info("Updated draft notification with id: {}", id);
        return updated;
    }

    private Commodity toCommodityDomain(CommodityFields commodityFields) {
        List<Species> species = commodityFields.species() != null
            ? commodityFields.species().stream()
                .map(this::toSpeciesDomain)
                .toList()
            : null; // Should never be null as at least one species is required
        return new Commodity(
            commodityFields.commodityCode(),
            commodityFields.description(),
            species,
            commodityFields.totalNetWeightKg(),
            commodityFields.totalNetFisheryProductWeightKg()
        );
    }

    private Species toSpeciesDomain(SpeciesFields speciesFields) {
        return new Species(
            speciesFields.speciesCode(),
            speciesFields.commonName(),
            speciesFields.scientificName(),
            speciesFields.netWeightKg(),
            speciesFields.netFisheryProductWeightKg()
        );
    }

    private CreationResult replay(NotificationAggregate notification, SaveNotificationRequest request) {
        var originalRequest = notification.getCreationRequest();
        if (originalRequest == null) {
            throw new IllegalStateException("Notification is missing its original creation request");
        }
        if (notification.getStatus() != NotificationStatus.DRAFT
            || !notification.getOrganisationId().equals(request.organisationId())
            || !originalRequest.organisationId().equals(request.organisationId())
            || originalRequest.notification().transport().mode() != request.notification().transport().mode()) {
            throw new ConflictException("Idempotency key has already been used with a different request");
        }
        log.info("Replayed draft creation with id: {}", notification.getId());
        NotificationAggregate original = new NotificationAggregate(notification.getId(), notification.getReference(),
            notification.getStatus(), notification.getOrganisationId(), notification.getCreatedAt(),
            notification.getCreatedAt(), originalRequest.notification().transport().mode());
        return new CreationResult(original, false);
    }
}
