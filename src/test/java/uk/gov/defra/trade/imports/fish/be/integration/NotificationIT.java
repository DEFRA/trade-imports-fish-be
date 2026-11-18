package uk.gov.defra.trade.imports.fish.be.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mongodb.ConnectionString;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.domain.repository.NotificationRepository;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class NotificationIT {
    private static MongoDBContainer mongoContainer;
    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationRepository repository;
    @Autowired private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        String uri = System.getProperty("test.mongodb.uri");
        String database;
        if (uri == null) {
            mongoContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoContainer.start();
            uri = mongoContainer.getReplicaSetUrl();
            database = "fisb72_" + UUID.randomUUID().toString().replace("-", "");
        } else {
            database = new ConnectionString(uri).getDatabase();
            if (database == null || !database.startsWith("fisb72_")) {
                throw new IllegalArgumentException("External test MongoDB URI must name an isolated fisb72_ database");
            }
        }
        String mongoUri = uri;
        String mongoDatabase = database;
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("spring.data.mongodb.database", () -> mongoDatabase);
        registry.add("spring.data.mongodb.ssl.enabled", () -> false);
    }

    @AfterAll
    static void stopMongoContainer() {
        if (mongoContainer != null) {
            mongoContainer.stop();
        }
    }

    @Test
    void firstSaveReplayAndUpdatesPreserveReferenceAndOrganisation(CapturedOutput output) throws Exception {
        String id = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        String response = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .header("x-cdp-request-id", traceId).contentType(MediaType.APPLICATION_JSON)
                .content(body("ship-or-vessel")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.organisationId").value("represented-importer"))
            .andExpect(jsonPath("$.createdAt").isString())
            .andExpect(jsonPath("$.updatedAt").isString())
            .andExpect(jsonPath("$.createdBy").doesNotExist())
            .andExpect(jsonPath("$.updatedBy").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        String reference = JsonPath.read(response, "$.reference");
        String createdAt = JsonPath.read(response, "$.createdAt");
        assertThat(Instant.parse(JsonPath.read(response, "$.updatedAt"))).isEqualTo(Instant.parse(createdAt));
        assertThat(reference).matches("GBN-FIS-\\d{2}-[0-9A-Z]{5}");
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("ship-or-vessel")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reference").value(reference))
            .andExpect(jsonPath("$.createdAt").value(createdAt))
            .andExpect(jsonPath("$.updatedAt").value(createdAt));
        for (String method : new String[]{"air-freight", "road-vehicle"}) {
            String updatedResponse = mockMvc.perform(put("/notifications/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON).content(body(method)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.transportMethod").value(method))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andReturn().getResponse().getContentAsString();
            Instant updatedAt = Instant.parse(JsonPath.read(updatedResponse, "$.updatedAt"));
            assertThat(updatedAt).isAfterOrEqualTo(Instant.parse(createdAt));
            assertThat(repository.findById(id).orElseThrow().getUpdatedAt()).isEqualTo(updatedAt);
        }
        mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.transportMethod").value("road-vehicle"));
        var stored = mongoTemplate.getCollection("notifications").find(new org.bson.Document("_id", id)).first();
        assertThat(stored).isNotNull();
        assertThat(stored.keySet()).contains("_id", "reference", "status", "organisationId",
            "createdAt", "updatedAt", "transportMethod")
            .doesNotContain("createdBy", "updatedBy", "created", "name", "value");
        assertThat(stored).containsEntry("createdAt", Date.from(Instant.parse(createdAt)));
        assertThat(stored.get("updatedAt")).isInstanceOf(Date.class);
        assertThat(output.toString()).contains(traceId).contains("Created draft notification");

        Notification duplicate = repository.findById(id).orElseThrow();
        duplicate.setId(UUID.randomUUID().toString());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.insert(duplicate))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void concurrentIdenticalFirstSavesCreateOneDraft() {
        String id = UUID.randomUUID().toString();
        var first = CompletableFuture.supplyAsync(() -> create(id));
        var second = CompletableFuture.supplyAsync(() -> create(id));
        assertThat(new int[]{first.join(), second.join()}).containsExactlyInAnyOrder(201, 200);
        assertThat(repository.findById(id)).isPresent();
        assertThat(mongoTemplate.getCollection("notifications").countDocuments(new org.bson.Document("_id", id)))
            .isEqualTo(1);
    }

    @Test
    void rejectsInvalidInputConflictingReplayAndOrganisationReassignment() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"organisationId\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.transportMethod").exists());
        assertThat(repository.findById(id)).isEmpty();
        assertThat(create(id)).isEqualTo(201);
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("road-vehicle")))
            .andExpect(status().isConflict());
        mockMvc.perform(put("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":\"another\",\"transportMethod\":\"road-vehicle\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/notifications/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void replayAfterUpdateMatchesOriginalCreationRequest() throws Exception {
        String id = UUID.randomUUID().toString();
        String created = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("air-freight")))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(put("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(body("road-vehicle")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("air-freight")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transportMethod").value("air-freight"))
            .andExpect(jsonPath("$.updatedAt").value((String) JsonPath.read(created, "$.createdAt")))
            .andExpect(jsonPath("$.creationTransportMethod").doesNotExist());
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("road-vehicle")))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.transportMethod").value("road-vehicle"));
    }

    @Test
    void rejectsNonStringOrganisationIdOnCreateAndUpdate() throws Exception {
        String id = UUID.randomUUID().toString();
        for (String organisationId : new String[]{"123", "true"}) {
            String invalid = "{\"organisationId\":" + organisationId + ",\"transportMethod\":\"air-freight\"}";
            mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
            mockMvc.perform(put("/notifications/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
        }
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void rejectsNonCanonicalUuidsAndTrailingJsonBeforePersistence() throws Exception {
        String shortForm = "1-1-1-1-1";
        mockMvc.perform(post("/notifications").header("Idempotency-Key", shortForm)
                .contentType(MediaType.APPLICATION_JSON).content(body("air-freight")))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/notifications/{id}", shortForm)).andExpect(status().isBadRequest());
        mockMvc.perform(put("/notifications/{id}", shortForm)
                .contentType(MediaType.APPLICATION_JSON).content(body("air-freight")))
            .andExpect(status().isBadRequest());
        assertThat(repository.findById("00000001-0001-0001-0001-000000000001")).isEmpty();

        String id = UUID.randomUUID().toString();
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("air-freight") + " trailing"))
            .andExpect(status().isBadRequest());
        assertThat(repository.findById(id)).isEmpty();
    }

    private int create(String id) {
        try {
            return mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("ship-or-vessel")))
                .andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to perform test request", exception);
        }
    }

    private String body(String method) {
        return "{\"organisationId\":\"represented-importer\",\"transportMethod\":\"" + method + "\"}";
    }
}
