package uk.gov.defra.trade.imports.fish.be.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.defra.trade.imports.fish.be.NotificationSchemaAssertions.assertAggregate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.ConnectionString;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
import uk.gov.defra.trade.imports.fish.be.domain.NotificationAggregate;
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
    @Autowired private MeterRegistry meterRegistry;

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

    @TestConfiguration
    static class FixedClockConfiguration {
        // Before the arrival dates used below, and before Mongo's currentDate used for updatedAt.
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void patchesArrivalAndPreservesTransport() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(create(id)).isEqualTo(201);

        String patched = mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(arrivalBody("FELIXSTOWE", "2027-06-15", "14:30")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.transport.mode").value("VESSEL"))
            .andExpect(jsonPath("$.notification.arrival.portOfEntry").value("FELIXSTOWE"))
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalDate").value("2027-06-15"))
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalTime").value("14:30"))
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalAt").value("2027-06-15T13:30:00Z"))
            .andExpect(jsonPath("$.notification.arrival.borderControlPost").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertAggregate(patched);

        String read = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        var mapper = new ObjectMapper();
        assertThat(mapper.readTree(read)).isEqualTo(mapper.readTree(patched));

        var arrival = mongoTemplate.getCollection("notifications").find(new org.bson.Document("_id", id)).first()
            .get("notification", org.bson.Document.class).get("arrival", org.bson.Document.class);
        assertThat(arrival).containsEntry("expectedArrivalDate", "2027-06-15")
            .containsEntry("expectedArrivalTime", "14:30")
            .containsEntry("expectedArrivalAt", Date.from(Instant.parse("2027-06-15T13:30:00Z")));
    }

    @Test
    void patchesRestOfWorldCountryAndChedReference() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(create(id)).isEqualTo(201);

        String patched = mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"organisationId":"represented-importer","notification":{
                      "countryOfExport":"NO","ched":{"reference":"CHEDP.GB.2026.1234567"},
                      "arrival":{"portOfEntry":"FELIXSTOWE","expectedArrivalDate":"2027-06-15",
                        "expectedArrivalTime":"14:30"}}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.transport.mode").value("VESSEL"))
            .andExpect(jsonPath("$.notification.countryOfExport").value("NO"))
            .andExpect(jsonPath("$.notification.exportRegion").value("REST_OF_WORLD"))
            .andExpect(jsonPath("$.notification.ched.reference").value("CHEDP.GB.2026.1234567"))
            .andExpect(jsonPath("$.notification.arrival.portOfEntry").value("FELIXSTOWE"))
            .andReturn().getResponse().getContentAsString();
        assertAggregate(patched);

        String read = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(new ObjectMapper().readTree(read)).isEqualTo(new ObjectMapper().readTree(patched));

        var stored = mongoTemplate.getCollection("notifications").find(new org.bson.Document("_id", id)).first();
        var notification = stored.get("notification", org.bson.Document.class);
        assertThat(notification).containsEntry("countryOfExport", "NO")
            .containsEntry("exportRegion", "REST_OF_WORLD");
        assertThat(notification.get("ched", org.bson.Document.class))
            .containsEntry("reference", "CHEDP.GB.2026.1234567");
    }

    @Test
    void changingToAnEuCountryRemovesTheStoredChedReference() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(create(id)).isEqualTo(201);
        mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"organisationId":"represented-importer","notification":{
                      "countryOfExport":"NO","ched":{"reference":"CHEDP.GB.2026.1234567"}}}
                    """))
            .andExpect(status().isOk());

        String patched = mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"organisationId":"represented-importer","notification":{"countryOfExport":"FR"}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.countryOfExport").value("FR"))
            .andExpect(jsonPath("$.notification.exportRegion").value("EU"))
            .andExpect(jsonPath("$.notification.ched").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertAggregate(patched);

        String read = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.ched").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertThat(new ObjectMapper().readTree(read)).isEqualTo(new ObjectMapper().readTree(patched));

        var stored = mongoTemplate.getCollection("notifications").find(new org.bson.Document("_id", id)).first();
        var notification = stored.get("notification", org.bson.Document.class);
        assertThat(notification).containsEntry("countryOfExport", "FR").containsEntry("exportRegion", "EU")
            .doesNotContainKey("ched");
    }

    @Test
    void canonicalCreationRetainsOriginalRequestAndBsonMappingOnReload() throws Exception {
        String id = UUID.randomUUID().toString();
        String originalRequest = body("AIR_FREIGHT");
        mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isNotFound());
        String created = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(originalRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.notification.transport.mode").value("AIR_FREIGHT"))
            .andReturn().getResponse().getContentAsString();
        assertAggregate(created);

        var stored = mongoTemplate.getCollection("notifications").find(new org.bson.Document("_id", id)).first();
        assertThat(stored).isNotNull();
        assertThat(stored.get("createdAt")).isInstanceOf(Date.class);
        assertThat(stored.get("updatedAt")).isEqualTo(stored.get("createdAt"));
        assertThat(stored.get("notification", org.bson.Document.class)
            .get("transport", org.bson.Document.class)).containsEntry("mode", "AIR_FREIGHT");
        assertThat(stored.get("creationRequest", org.bson.Document.class))
            .isEqualTo(org.bson.Document.parse(originalRequest));
        assertThat(stored).doesNotContainKeys("transportMethod", "creationTransportMethod", "creationTransportMode");

        String read = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String replay = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(originalRequest))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var mapper = new ObjectMapper();
        assertThat(mapper.readTree(read)).isEqualTo(mapper.readTree(created));
        assertThat(mapper.readTree(replay)).isEqualTo(mapper.readTree(created));
        assertAggregate(read);
        assertAggregate(replay);
    }

    @Test
    void combinedPatchOverwritesArrivalAndKeepsBorderControlPost() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(create(id)).isEqualTo(201);
        mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(arrivalBody("FELIXSTOWE", "2027-06-15", "14:30")))
            .andExpect(status().isOk());
        mongoTemplate.getCollection("notifications").updateOne(new org.bson.Document("_id", id),
            new org.bson.Document("$set", new org.bson.Document("notification.arrival.borderControlPost",
                "FELIXSTOWE-BCP")));

        String patched = mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"organisationId":"represented-importer","notification":{
                      "transport":{"mode":"AIR_FREIGHT"},
                      "arrival":{"portOfEntry":"HEATHROW","expectedArrivalDate":"2026-12-24","expectedArrivalTime":"23:15"}}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.transport.mode").value("AIR_FREIGHT"))
            .andExpect(jsonPath("$.notification.arrival.portOfEntry").value("HEATHROW"))
            .andExpect(jsonPath("$.notification.arrival.borderControlPost").value("FELIXSTOWE-BCP"))
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalDate").value("2026-12-24"))
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalTime").value("23:15"))
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalAt").value("2026-12-24T23:15:00Z"))
            .andReturn().getResponse().getContentAsString();
        assertAggregate(patched);
        String read = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        var mapper = new ObjectMapper();
        assertThat(mapper.readTree(read)).isEqualTo(mapper.readTree(patched));
    }

    @Test
    void pastArrivalIsRejectedWithoutChangingTheDraft() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(create(id)).isEqualTo(201);
        String before = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(arrivalBody("FELIXSTOWE", "2026-10-31", "23:59")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors['notification.arrival.expectedArrivalTime']").exists());

        String after = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        var mapper = new ObjectMapper();
        assertThat(mapper.readTree(after)).isEqualTo(mapper.readTree(before));

        // Exactly the fixed clock's instant is not in the past, which also shows the validator uses that clock.
        mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(arrivalBody("FELIXSTOWE", "2026-11-01", "00:00")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalAt").value("2026-11-01T00:00:00Z"));
    }

    @Test
    void arrivalTimeRepeatedByUkClocksGoingBackUsesTheEarlierInstant() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(create(id)).isEqualTo(201);
        mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(arrivalBody("FELIXSTOWE", "2027-10-31", "01:30")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.arrival.expectedArrivalAt").value("2027-10-31T00:30:00Z"));
    }

    @Test
    void patchesOnlyModeAndPreservesFullNotificationContent() throws Exception {
        String id = UUID.randomUUID().toString();
        String created = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("VESSEL")))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var additions = org.bson.Document.parse("""
            {
              "importerOrganisationId":"represented-importer",
              "createdBy":{"userId":"test-user","actingAs":"IMPORTER"},
              "concurrencyToken":0,
              "notification":{
                "transport":{
                  "mode":"VESSEL","vesselName":"TEST VESSEL","imoNumber":"9876543",
                  "flightNumber":"TEST123","vehicleRegistration":"TEST REG",
                  "containerNumbers":["MSCU1102101"],"transportDocumentReference":"BL-TEST"
                },
                "arrival":{
                  "portOfEntry":"FELIXSTOWE","borderControlPost":"FELIXSTOWE-BCP",
                  "expectedArrivalDate":"2026-11-30","expectedArrivalTime":"06:00",
                  "expectedArrivalAt":"2026-11-30T06:00:00Z"
                },
                "landingType":"CONTAINERISED","countryOfExport":"IS","exportRegion":"REST_OF_WORLD",
                "ched":{"reference":"CHEDP.GB.2026.1234567"},
                "commodities":[{
                  "commodityCode":"03024400","description":"Mackerel",
                  "species":[{"speciesCode":"MAC","commonName":"Mackerel","netWeightKg":12.5}],
                  "totalNetWeightKg":12.5
                }],
                "importer":{"name":"Test importer","address":{"countryCode":"GB","townOrCity":"Test town"}},
                "agent":{"name":"Test agent"},"responsibleParty":"IMPORTER",
                "exporter":{"name":"Test exporter"},
                "documents":[{"documentId":"doc-1","documentType":"ADDITIONAL","reviewStatus":"NEEDS_REVIEW"}],
                "declaration":{"optionVersion":"test-v1","statements":[{"code":"TEST","accepted":false}]}
              },
              "fieldProvenance":[{
                "path":"transport.vesselName","source":"EXTRACTED","documentId":"doc-1",
                "confidence":0.5,"originalValue":null,"changedAt":"2026-11-23T20:00:00Z"
              }]
            }
            """);
        var mapper = new ObjectMapper();
        ObjectNode expected = mapper.readValue(created, ObjectNode.class);
        expected.setAll(mapper.readValue(additions.toJson(), ObjectNode.class));
        ((ObjectNode) expected.path("notification").path("transport")).put("mode", "AIR_FREIGHT");
        additions.get("notification", org.bson.Document.class).get("arrival", org.bson.Document.class)
            .put("expectedArrivalAt", Date.from(Instant.parse("2026-11-30T06:00:00Z")));
        mongoTemplate.getCollection("notifications").updateOne(new org.bson.Document("_id", id),
            new org.bson.Document("$set", additions));
        Instant previousSaveTime = Instant.parse(expected.path("updatedAt").asText());

        for (int save = 0; save < 2; save++) {
            String patched = mockMvc.perform(patch("/notifications/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON).content(body("AIR_FREIGHT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var actual = mapper.readTree(patched);
            Instant saveTime = Instant.parse(actual.path("updatedAt").asText());
            assertThat(saveTime).isAfterOrEqualTo(previousSaveTime);
            expected.set("updatedAt", actual.path("updatedAt"));
            assertThat(actual).isEqualTo(expected);
            assertAggregate(patched);
            String read = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            assertThat(mapper.readTree(read)).isEqualTo(expected);
            previousSaveTime = saveTime;
        }
    }

    @Test
    void patchRejectsNonDraftWithoutDeserializingItsStatus() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("AIR_FREIGHT")))
            .andExpect(status().isCreated());
        var collection = mongoTemplate.getCollection("notifications");
        var identity = new org.bson.Document("_id", id);
        collection.updateOne(identity, new org.bson.Document("$set", new org.bson.Document("status", "SUBMITTED")));
        var stored = collection.find(identity).first();

        mockMvc.perform(patch("/notifications/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("ROAD_VEHICLE")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Resource Conflict"));
        assertThat(collection.find(identity).first()).isEqualTo(stored);
    }

    @Test
    void firstSaveReplayAndUpdatesPreserveReferenceAndOrganisation(CapturedOutput output) throws Exception {
        double createdBefore = meterRegistry.counter("notification_created").count();
        double updatedBefore = meterRegistry.counter("notification_updated").count();
        String id = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        String response = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .header("x-cdp-request-id", traceId).contentType(MediaType.APPLICATION_JSON)
                .content(body("VESSEL")))
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
                .contentType(MediaType.APPLICATION_JSON).content(body("VESSEL")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reference").value(reference))
            .andExpect(jsonPath("$.createdAt").value(createdAt))
            .andExpect(jsonPath("$.updatedAt").value(createdAt));
        Instant previousUpdatedAt = Instant.parse(createdAt);
        mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.reference").value(reference))
            .andExpect(jsonPath("$.notification.transport.mode").value("VESSEL"));
        for (String mode : new String[]{"AIR_FREIGHT", "ROAD_VEHICLE"}) {
            String updatedResponse = mockMvc.perform(patch("/notifications/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON).content(body(mode)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.notification.transport.mode").value(mode))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andReturn().getResponse().getContentAsString();
            Instant updatedAt = Instant.parse(JsonPath.read(updatedResponse, "$.updatedAt"));
            assertThat(updatedAt).isAfterOrEqualTo(previousUpdatedAt);
            assertThat(repository.findById(id).orElseThrow().getUpdatedAt()).isEqualTo(updatedAt);
            assertAggregate(updatedResponse);
            previousUpdatedAt = updatedAt;
        }
        assertThat(counterValue("notification_created")).isEqualTo(createdBefore + 1);
        assertThat(counterValue("notification_updated")).isEqualTo(updatedBefore + 2);
        mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.transport.mode").value("ROAD_VEHICLE"));
        var stored = mongoTemplate.getCollection("notifications").find(new org.bson.Document("_id", id)).first();
        assertThat(stored).isNotNull();
        assertThat(stored.keySet()).contains("_id", "reference", "status", "organisationId",
            "createdAt", "updatedAt", "notification", "creationRequest")
            .doesNotContain("transportMethod", "creationTransportMethod", "createdBy", "updatedBy", "created", "name", "value");
        assertThat(stored).containsEntry("createdAt", Date.from(Instant.parse(createdAt)));
        assertThat(stored.get("updatedAt")).isInstanceOf(Date.class);
        assertThat(output.toString()).contains(traceId).contains("Created draft notification");

        NotificationAggregate duplicate = repository.findById(id).orElseThrow();
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
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":\"\",\"notification\":{\"transport\":{}}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors['notification.transport.mode']").exists());
        assertThat(repository.findById(id)).isEmpty();
        assertThat(create(id)).isEqualTo(201);
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("ROAD_VEHICLE")))
            .andExpect(status().isConflict());
        mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organisationId\":\"another\",\"notification\":{\"transport\":{\"mode\":\"ROAD_VEHICLE\"}}}"))
            .andExpect(status().isConflict());
        UUID missing = UUID.randomUUID();
        mockMvc.perform(patch("/notifications/{id}", missing).contentType(MediaType.APPLICATION_JSON)
                .content(body("ROAD_VEHICLE"))).andExpect(status().isNotFound());
        mockMvc.perform(get("/notifications/{id}", missing)).andExpect(status().isNotFound());
        mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.organisationId").value("represented-importer"))
            .andExpect(jsonPath("$.notification.transport.mode").value("VESSEL"));
    }

    @Test
    void replayAfterUpdateMatchesOriginalCreationRequest() throws Exception {
        String id = UUID.randomUUID().toString();
        String created = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("AIR_FREIGHT")))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String patched = mockMvc.perform(patch("/notifications/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(body("ROAD_VEHICLE")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String replay = mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("AIR_FREIGHT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.transport.mode").value("AIR_FREIGHT"))
            .andExpect(jsonPath("$.updatedAt").value((String) JsonPath.read(created, "$.createdAt")))
            .andExpect(jsonPath("$.creationRequest").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        var mapper = new ObjectMapper();
        assertThat(mapper.readTree(replay)).isEqualTo(mapper.readTree(created));
        assertAggregate(replay);
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("ROAD_VEHICLE")))
            .andExpect(status().isConflict());
        String read = mockMvc.perform(get("/notifications/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.notification.transport.mode").value("ROAD_VEHICLE"))
            .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(read)).isEqualTo(mapper.readTree(patched));
        assertAggregate(read);
    }

    @Test
    void rejectsNonStringOrganisationIdOnCreateAndUpdate() throws Exception {
        String id = UUID.randomUUID().toString();
        for (String organisationId : new String[]{"123", "true"}) {
            String invalid = "{\"organisationId\":" + organisationId
                + ",\"notification\":{\"transport\":{\"mode\":\"AIR_FREIGHT\"}}}";
            mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
            mockMvc.perform(patch("/notifications/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
        }
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void rejectsNonCanonicalUuidsAndTrailingJsonBeforePersistence() throws Exception {
        String shortForm = "1-1-1-1-1";
        mockMvc.perform(post("/notifications").header("Idempotency-Key", shortForm)
                .contentType(MediaType.APPLICATION_JSON).content(body("AIR_FREIGHT")))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/notifications/{id}", shortForm)).andExpect(status().isBadRequest());
        mockMvc.perform(patch("/notifications/{id}", shortForm)
                .contentType(MediaType.APPLICATION_JSON).content(body("AIR_FREIGHT")))
            .andExpect(status().isBadRequest());
        assertThat(repository.findById("00000001-0001-0001-0001-000000000001")).isEmpty();

        String id = UUID.randomUUID().toString();
        mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("AIR_FREIGHT") + " trailing"))
            .andExpect(status().isBadRequest());
        assertThat(repository.findById(id)).isEmpty();
    }

    private int create(String id) {
        try {
            return mockMvc.perform(post("/notifications").header("Idempotency-Key", id)
                .contentType(MediaType.APPLICATION_JSON).content(body("VESSEL")))
                .andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to perform test request", exception);
        }
    }

    private String body(String mode) {
        return "{\"organisationId\":\"represented-importer\",\"notification\":{\"transport\":{\"mode\":\"" + mode + "\"}}}";
    }

    private String arrivalBody(String port, String date, String time) {
        return "{\"organisationId\":\"represented-importer\",\"notification\":{\"arrival\":{\"portOfEntry\":\""
            + port + "\",\"expectedArrivalDate\":\"" + date + "\",\"expectedArrivalTime\":\"" + time + "\"}}}";
    }

    private double counterValue(String name) {
        var counter = meterRegistry.find(name).counter();
        assertThat(counter).as("%s must be enabled by the metrics allowlist", name).isNotNull();
        return counter.count();
    }
}
