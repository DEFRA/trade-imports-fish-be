package uk.gov.defra.trade.imports.fish.be.controller;

import static uk.gov.defra.trade.imports.fish.be.NotificationSchemaAssertions.assertAggregate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.defra.trade.imports.fish.be.configuration.StrictUuidBindingAdvice;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationStatus;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationContent;
import uk.gov.defra.trade.imports.fish.be.domain.TransportMode;
import uk.gov.defra.trade.imports.fish.be.exceptions.ConflictException;
import uk.gov.defra.trade.imports.fish.be.exceptions.GlobalExceptionHandler;
import uk.gov.defra.trade.imports.fish.be.exceptions.NotFoundException;
import uk.gov.defra.trade.imports.fish.be.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock
    private NotificationService service;
    private MockMvc mvc;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
        mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service))
            .setControllerAdvice(new GlobalExceptionHandler(), new StrictUuidBindingAdvice())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VESSEL", "AIR_FREIGHT", "ROAD_VEHICLE"})
    void createsEachTransportMode(String mode) throws Exception {
        Notification notification = notification();
        when(service.create(any(), any())).thenAnswer(invocation -> {
            var request = invocation.getArgument(1,
                uk.gov.defra.trade.imports.fish.be.controller.dto.SaveNotificationRequest.class);
            notification.setNotification(new NotificationContent(
                new NotificationContent.Transport(request.notification().transport().mode())));
            return new NotificationService.CreationResult(notification, true);
        });
        String response = mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"" + mode + "\"}}}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/notifications/" + id))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.notification.transport.mode").value(mode))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.createdAt").value("2026-11-16T16:00:00.123Z"))
            .andExpect(jsonPath("$.updatedAt").value("2026-11-16T16:00:00.123Z"))
            .andExpect(jsonPath("$.createdBy").doesNotExist())
            .andExpect(jsonPath("$.updatedBy").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertAggregate(response);
    }

    @Test
    void createsCanonicalTransportDraft() throws Exception {
        when(service.create(any(), any())).thenReturn(new NotificationService.CreationResult(notification(), true));
        String response = mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"organisationId":"importer","notification":{"transport":{"mode":"AIR_FREIGHT"}}}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/notifications/" + id))
            .andExpect(jsonPath("$.notification.transport.mode").value("AIR_FREIGHT"))
            .andExpect(jsonPath("$.transportMethod").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertAggregate(response);
    }

    @Test
    void rejectsWhitespacePaddedTransportMode() throws Exception {
        String body = """
            {"organisationId":"importer","notification":{"transport":{"mode":" AIR_FREIGHT "}}}
            """;
        mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void identicalReplayReturnsOk() throws Exception {
        when(service.create(any(), any())).thenReturn(new NotificationService.CreationResult(notification(), false));
        mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isOk())
            .andExpect(header().string("Location", "/notifications/" + id));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"organisationId\":\" \",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}",
        "{\"organisationId\":\"importer\"}",
        "{\"organisationId\":\"importer\",\"notification\":null}",
        "{\"organisationId\":\"importer\",\"notification\":{}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":null}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":null}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"air-freight\"}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":1}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"1\"}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":[\"AIR_FREIGHT\"]}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\",\"vesselName\":\"TEST\"}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"},\"arrival\":{}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}},\"status\":\"DRAFT\"}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}},\"id\":\"owned\"}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}},\"createdAt\":\"2026-01-01T00:00:00Z\"}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}},\"updatedAt\":\"2026-01-01T00:00:00Z\"}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}},\"creationRequest\":{}}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\"}",
        "{\"organisationId\":123,\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}",
        "{\"organisationId\":1.5,\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}",
        "{\"organisationId\":true,\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}",
        "{\"organisationId\":{\"id\":\"importer\"},\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}{\"organisationId\":\"other\"}",
        "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}} garbage",
        "{broken"
    })
    void invalidBodiesReturnProblemDetailWithoutPersistence(String body) throws Exception {
        mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Validation Error"));
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Validation Error"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"notification\":null}",
        "{\"notification\":{\"transport\":null}}",
        "{\"notification\":{\"arrival\":null}}",
        "{\"notification\":{\"arrival\":{}}}",
        "{\"notification\":{\"arrival\":{\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":null,\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"felixstowe\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\" FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":123,\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-6-15\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"15/06/2099\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-02-30\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15T14:30\",\"expectedArrivalTime\":\"14:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"9:30\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"09:30:00\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"24:00\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:60\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\",\"borderControlPost\":\"FELIXSTOWE-BCP\"}}}",
        "{\"notification\":{\"arrival\":{\"portOfEntry\":\"FELIXSTOWE\",\"expectedArrivalDate\":\"2099-06-15\",\"expectedArrivalTime\":\"14:30\",\"expectedArrivalAt\":\"2099-06-15T13:30:00Z\"}}}"
    })
    void invalidArrivalPatchesAreRejectedWithoutPersistence(String notification) throws Exception {
        String body = "{\"organisationId\":\"importer\"," + notification.substring(1);
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Validation Error"));
        verifyNoInteractions(service);
    }

    @Test
    void impossibleArrivalDateReportsItsNestedValidationPath() throws Exception {
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"organisationId":"importer","notification":{"arrival":{
                  "portOfEntry":"FELIXSTOWE","expectedArrivalDate":"2099-02-30","expectedArrivalTime":"14:30"}}}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors['notification.arrival.expectedArrivalDate']").exists());
        verifyNoInteractions(service);
    }

    @Test
    void arrivalTimeSkippedByUkClocksGoingForwardIsRejected() throws Exception {
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"organisationId":"importer","notification":{"arrival":{
                  "portOfEntry":"FELIXSTOWE","expectedArrivalDate":"2099-03-29","expectedArrivalTime":"01:30"}}}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors['notification.arrival.expectedArrivalTime']").exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1.5", "true", "false"})
    void nonStringOrganisationIdIsRejectedOnUpdate(String organisationId) throws Exception {
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":" + organisationId
                    + ",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"));
        verifyNoInteractions(service);
    }

    @Test
    void missingModeReportsItsNestedValidationPath() throws Exception {
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":\"importer\",\"notification\":{\"transport\":{}}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors['notification.transport.mode']").value("Transport mode is required"));
        verifyNoInteractions(service);
    }

    @Test
    void readsIncompleteCanonicalDraftWithoutRequiringTransport() throws Exception {
        Notification draft = notification();
        draft.setNotification(new NotificationContent(null));
        when(service.findById(id)).thenReturn(draft);
        String response = mvc.perform(get("/notifications/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification").isMap())
            .andExpect(jsonPath("$.notification.transport").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertAggregate(response);
    }

    @Test
    void patchDoesNotAcceptJsonMergePatchMediaType() throws Exception {
        mvc.perform(patch("/notifications/{id}", id)
                .contentType("application/merge-patch+json").content(validBody()))
            .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(service);
    }

    @Test
    void oldPutRouteIsNotRetained() throws Exception {
        mvc.perform(put("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    @Test
    void missingOrInvalidIdentifiersReturnBadRequest() throws Exception {
        mvc.perform(post("/notifications").contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/notifications").header("Idempotency-Key", "invalid")
                .contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/notifications/invalid")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void trailingContentIsRejectedOnUpdate() throws Exception {
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(validBody() + "{\"organisationId\":\"other\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1-1-1-1-1", "00000001-0001-0001-0001-1", "{12345678-1234-1234-1234-123456789012}",
        "12345678123412341234123456789012", "1234567g-1234-1234-1234-123456789012"})
    void nonCanonicalUuidsAreRejectedWithoutServiceInteraction(String uuid) throws Exception {
        mvc.perform(post("/notifications").header("Idempotency-Key", uuid)
                .contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Validation Error"));
        mvc.perform(get("/notifications/{id}", uuid)).andExpect(status().isBadRequest());
        mvc.perform(patch("/notifications/{id}", uuid).contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void upperCaseCanonicalUuidIsAccepted() throws Exception {
        when(service.findById(id)).thenReturn(notification());
        mvc.perform(get("/notifications/{id}", id.toString().toUpperCase()))
            .andExpect(status().isOk());
    }

    @Test
    void readsAndUpdatesExistingDraft() throws Exception {
        when(service.findById(id)).thenReturn(notification());
        when(service.update(any(), any())).thenReturn(notification());
        mvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.reference").value("GBN-FIS-26-AXN04"));
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void missingDraftAndConflictsUseExistingProblemDetails() throws Exception {
        when(service.findById(id)).thenThrow(new NotFoundException("Missing draft"));
        mvc.perform(get("/notifications/{id}", id)).andExpect(status().isNotFound());
        when(service.update(any(), any())).thenThrow(new ConflictException("Different organisation"));
        mvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    private Notification notification() {
        return new Notification(id.toString(), "GBN-FIS-26-AXN04", NotificationStatus.DRAFT,
            "importer", Instant.parse("2026-11-16T16:00:00.123Z"),
            Instant.parse("2026-11-16T16:00:00.123Z"), TransportMode.AIR_FREIGHT);
    }

    private String validBody() {
        return "{\"organisationId\":\"importer\",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}";
    }
}
