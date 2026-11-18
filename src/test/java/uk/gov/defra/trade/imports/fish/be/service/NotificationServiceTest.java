package uk.gov.defra.trade.imports.fish.be.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.defra.trade.imports.fish.be.controller.dto.SaveNotificationRequest;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationStatus;
import uk.gov.defra.trade.imports.fish.be.domain.TransportMethod;
import uk.gov.defra.trade.imports.fish.be.domain.repository.NotificationRepository;
import uk.gov.defra.trade.imports.fish.be.exceptions.ConflictException;
import uk.gov.defra.trade.imports.fish.be.exceptions.NotFoundException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-16T16:00:00.123Z");
    @Mock private NotificationRepository repository;
    @Mock private ReferenceNumberGenerator generator;
    @Mock private MongoTemplate mongoTemplate;
    private NotificationService service;
    private SimpleMeterRegistry meterRegistry;
    private final UUID id = UUID.randomUUID();
    private final SaveNotificationRequest request = new SaveNotificationRequest("importer", TransportMethod.AIR_FREIGHT);

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new NotificationService(repository, generator, mongoTemplate, meterRegistry,
            Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
    }

    @Test
    void createsMinimalDraftWithServerOwnedFields() {
        when(generator.generate()).thenReturn("GBN-FIS-26-AXN04");
        when(repository.insert(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(id, request);
        assertThat(result.created()).isTrue();
        assertThat(result.notification()).isEqualTo(notification());
    }

    @Test
    void replayKeepsIdentityWithoutAnotherInsert() {
        Instant originalTime = NOW.minusSeconds(60);
        Notification existing = notification();
        existing.setCreatedAt(originalTime);
        existing.setUpdatedAt(originalTime);
        when(repository.findById(id.toString())).thenReturn(Optional.of(existing));
        var result = service.create(id, request);
        assertThat(result.created()).isFalse();
        assertThat(result.notification().getReference()).isEqualTo("GBN-FIS-26-AXN04");
        assertThat(result.notification().getCreatedAt()).isEqualTo(originalTime);
        assertThat(result.notification().getUpdatedAt()).isEqualTo(originalTime);
        verify(repository, never()).insert(any(Notification.class));
    }

    @Test
    void rejectsReplayWithDifferentOrganisationOrTransport() {
        when(repository.findById(id.toString())).thenReturn(Optional.of(notification()));
        var differentOrganisation = new SaveNotificationRequest("another", TransportMethod.AIR_FREIGHT);
        var differentTransport = new SaveNotificationRequest("importer", TransportMethod.ROAD_VEHICLE);
        assertThatThrownBy(() -> service.create(id, differentOrganisation))
            .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.create(id, differentTransport))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void replayAfterUpdateIsValidatedAgainstAndReturnsOriginalCreation() {
        Notification updated = notification();
        updated.setTransportMethod(TransportMethod.ROAD_VEHICLE);
        updated.setUpdatedAt(NOW.plusSeconds(60));
        when(repository.findById(id.toString())).thenReturn(Optional.of(updated));

        var result = service.create(id, request);
        assertThat(result.created()).isFalse();
        assertThat(result.notification()).isEqualTo(notification());

        var updatedPayload = new SaveNotificationRequest("importer", TransportMethod.ROAD_VEHICLE);
        assertThatThrownBy(() -> service.create(id, updatedPayload))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void concurrentCreationReturnsTheWinningDraft() {
        when(repository.findById(id.toString()))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(notification()));
        when(generator.generate()).thenReturn("GBN-FIS-26-OTHER");
        when(repository.insert(any(Notification.class))).thenThrow(new DuplicateKeyException("duplicate id"));
        var result = service.create(id, request);
        assertThat(result.created()).isFalse();
        assertThat(result.notification().getReference()).isEqualTo("GBN-FIS-26-AXN04");
    }

    @Test
    void referenceCollisionRetriesWithoutChangingIdentity() {
        when(generator.generate()).thenReturn("GBN-FIS-26-CLASH", "GBN-FIS-26-AXN04");
        when(repository.insert(any(Notification.class)))
            .thenThrow(new DuplicateKeyException("duplicate reference"))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.existsByReference("GBN-FIS-26-CLASH")).thenReturn(true);
        var result = service.create(id, request);
        assertThat(result.notification()).isEqualTo(notification());
    }

    @Test
    void referenceCollisionRetriesAreBoundedAndOtherDuplicateFailuresPropagate() {
        when(generator.generate()).thenReturn("GBN-FIS-26-CLASH");
        when(repository.insert(any(Notification.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.existsByReference("GBN-FIS-26-CLASH")).thenReturn(true);
        assertThatThrownBy(() -> service.create(id, request)).isInstanceOf(IllegalStateException.class);
        when(repository.existsByReference("GBN-FIS-26-CLASH")).thenReturn(false);
        assertThatThrownBy(() -> service.create(id, request)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void updateAtomicallyChangesOnlyTransportAndUpdatedAt() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
            org.mockito.ArgumentMatchers.eq(Notification.class))).thenReturn(notification());
        assertThat(service.update(id, request).getReference()).isEqualTo("GBN-FIS-26-AXN04");
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(query.capture(), update.capture(), any(FindAndModifyOptions.class),
            org.mockito.ArgumentMatchers.eq(Notification.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", id.toString())
            .containsEntry("organisationId", "importer").containsEntry("status", NotificationStatus.DRAFT);
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class))
            .containsOnlyKeys("transportMethod", "updatedAt")
            .containsEntry("updatedAt", NOW);
    }

    @Test
    void creationTruncatesTimestampToMongoDbMillisecondPrecision() {
        Instant preciseTime = Instant.parse("2026-11-16T16:00:00.123456789Z");
        service = new NotificationService(repository, generator, mongoTemplate, meterRegistry,
            Clock.fixed(preciseTime, ZoneOffset.UTC));
        when(generator.generate()).thenReturn("GBN-FIS-26-AXN04");
        when(repository.insert(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(id, request);
        assertThat(result.notification().getCreatedAt()).isEqualTo(NOW);
        assertThat(result.notification().getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void missingUpdateIsNotAnUpsertAndOrganisationMismatchIsConflict() {
        assertThatThrownBy(() -> service.update(id, request)).isInstanceOf(NotFoundException.class);
        when(repository.findById(id.toString())).thenReturn(Optional.of(notification()));
        assertThatThrownBy(() -> service.update(id, request)).isInstanceOf(ConflictException.class);
        verify(repository, never()).insert(any(Notification.class));
    }

    private Notification notification() {
        return new Notification(id.toString(), "GBN-FIS-26-AXN04", NotificationStatus.DRAFT,
            "importer", NOW, NOW, TransportMethod.AIR_FREIGHT);
    }
}
