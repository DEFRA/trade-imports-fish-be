package uk.gov.defra.trade.imports.fish.be.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.defra.trade.imports.fish.be.configuration.StrictUuidBindingAdvice;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.domain.NotificationStatus;
import uk.gov.defra.trade.imports.fish.be.domain.TransportMethod;
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
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
        mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service))
            .setControllerAdvice(new GlobalExceptionHandler(), new StrictUuidBindingAdvice())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ship-or-vessel", "air-freight", "road-vehicle"})
    void createsEachTransportMethod(String method) throws Exception {
        Notification notification = notification();
        when(service.create(any(), any())).thenAnswer(invocation -> {
            var request = invocation.getArgument(1,
                uk.gov.defra.trade.imports.fish.be.controller.dto.SaveNotificationRequest.class);
            notification.setTransportMethod(request.transportMethod());
            return new NotificationService.CreationResult(notification, true);
        });
        mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":\"importer\",\"transportMethod\":\"" + method + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/notifications/" + id))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.transportMethod").value(method))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.createdAt").value("2026-11-16T16:00:00.123Z"))
            .andExpect(jsonPath("$.updatedAt").value("2026-11-16T16:00:00.123Z"))
            .andExpect(jsonPath("$.createdBy").doesNotExist())
            .andExpect(jsonPath("$.updatedBy").doesNotExist());
    }

    @Test
    void identicalReplayReturnsOk() throws Exception {
        when(service.create(any(), any())).thenReturn(new NotificationService.CreationResult(notification(), false));
        mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"organisationId\":\" \",\"transportMethod\":\"air-freight\"}",
        "{\"organisationId\":\"importer\"}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"train\"}",
        "{\"organisationId\":\"importer\",\"transportMethod\":1}",
        "{\"organisationId\":\"importer\",\"transportMethod\":[\"air-freight\"]}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\",\"status\":\"DRAFT\"}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\",\"id\":\"owned\"}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\",\"createdAt\":\"2026-01-01T00:00:00Z\"}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\",\"updatedAt\":\"2026-01-01T00:00:00Z\"}",
        "{\"organisationId\":123,\"transportMethod\":\"air-freight\"}",
        "{\"organisationId\":1.5,\"transportMethod\":\"air-freight\"}",
        "{\"organisationId\":true,\"transportMethod\":\"air-freight\"}",
        "{\"organisationId\":{\"id\":\"importer\"},\"transportMethod\":\"air-freight\"}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\"}{\"organisationId\":\"other\"}",
        "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\"} garbage",
        "{broken"
    })
    void invalidBodiesReturnProblemDetailWithoutPersistence(String body) throws Exception {
        mvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1.5", "true", "false"})
    void nonStringOrganisationIdIsRejectedOnUpdate(String organisationId) throws Exception {
        mvc.perform(put("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":" + organisationId + ",\"transportMethod\":\"air-freight\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"));
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
        mvc.perform(put("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
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
        mvc.perform(put("/notifications/{id}", uuid).contentType(MediaType.APPLICATION_JSON).content(validBody()))
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
        mvc.perform(put("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void missingDraftAndConflictsUseExistingProblemDetails() throws Exception {
        when(service.findById(id)).thenThrow(new NotFoundException("Missing draft"));
        mvc.perform(get("/notifications/{id}", id)).andExpect(status().isNotFound());
        when(service.update(any(), any())).thenThrow(new ConflictException("Different organisation"));
        mvc.perform(put("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validBody()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    private Notification notification() {
        return new Notification(id.toString(), "GBN-FIS-26-AXN04", NotificationStatus.DRAFT,
            "importer", Instant.parse("2026-11-16T16:00:00.123Z"),
            Instant.parse("2026-11-16T16:00:00.123Z"), TransportMethod.AIR_FREIGHT);
    }

    private String validBody() {
        return "{\"organisationId\":\"importer\",\"transportMethod\":\"air-freight\"}";
    }
}
