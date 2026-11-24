# Transport draft alignment: API contract and test-first implementation

This document records the backend-only design agreed on 23 November 2026 and implemented on 24 November 2026. The POST/GET/PATCH contract below is implemented; frontend integration and shared-environment deployment remain separate work.

## Investigated baseline

- [Schema PR #6](https://github.com/DEFRA/trade-imports-fish-be/pull/6) merged on 19 November 2026.
- [Transport PR #7](https://github.com/DEFRA/trade-imports-fish-be/pull/7) remains open and unmerged. Its head is `68b6de603efa8559a8612f3857089e2020f48738`, on `feature/FISB-72-transport-details`.
- The inspected backend branch, `feature/FISB-73-arrival-details`, points at that same commit. It has no arrival implementation changes.
- The existing controller accepts flat `organisationId` / `transportMethod` requests and returns the MongoDB-mapped domain object. The service already provides first-save idempotency, reference collision retries and guarded atomic updates.
- The existing `creationTransportMethod` is persisted but hidden from JSON. A creation replay after an update reconstructs the original response; a read returns current state.
- The aggregate and submitted schemas are present, but no runtime code reads them. The Java status enum currently contains only `DRAFT`; schema-defined submission/deletion states are not implemented.
- The accessible frontend was inspected at commit `56cd39d`, on `feature/FISB-73-consignment-details`. Its client validates flat responses; its session service recovers a committed creation whose response was lost using GET before save. It was not modified.
- No glossary or ADR directory existed on the inspected backend branch. The accompanying [glossary](../GLOSSARY.md) and [ADRs](adr/) establish the agreed terminology and architectural decisions.

## Agreed scope

1. Use `VESSEL`, `AIR_FREIGHT` and `ROAD_VEHICLE`, stored and returned as `notification.transport.mode`.
2. Accept small nested transport-only creation and patch requests. Do not require the whole aggregate.
3. Return the full canonical notification representation on every successful operation, with the historical creation representation on replay.
4. Introduce one general PATCH endpoint with a strict initial allowlist containing only transport mode.
5. Separate API request/response DTOs from persistence representation, retaining immutable creation-request metadata privately in the existing record.
6. Preserve identity, ownership, references, timestamps and successful-save/recovery behavior.
7. Validate requests strictly and test canonical JSON against the aggregate schema. Runtime aggregate validation remains separate work.

### Explicit non-goals

- Arrival saving (since added by FISB-73; see [Arrival details](#arrival-details)), submission, deletion or additional lifecycle behavior.
- Writers for document-derived fields or other notification sections.
- Transport-detail clearing or reconciliation, including provenance reconciliation.
- New UI fields or any frontend implementation.
- New concurrency controls, generated version tokens, replacement writes or upserts.
- Temporary compatibility with the flat contract, a compatibility reader or legacy-data migration.
- Changes to authentication/authorisation, reference generation or unrelated error handling.
- Provisioning environments, deployments, traffic pauses, rollback execution or data deletion.

Schema-defined optional fields may be retained and returned without making them writable in this ticket. This is not an implementation of every schema feature.

## HTTP contract

| Operation | Endpoint | Success | Expected request/domain errors |
| --- | --- | --- | --- |
| First save | `POST /notifications` | `201 Created` | `400` invalid request; `409` creation-key conflict |
| Identical creation replay | `POST /notifications` | `200 OK` | `400` invalid request; `409` creation-key conflict |
| Read | `GET /notifications/{id}` | `200 OK` | `400` invalid UUID; `404` missing notification |
| Save transport mode and/or arrival | `PATCH /notifications/{id}` | `200 OK` | `400` invalid request; `404` missing notification; `409` ownership/state conflict |

POST requires the existing canonical journey UUID in `Idempotency-Key`. That UUID becomes the notification ID. POST success, including replay, retains `Location: /notifications/{id}`.

GET has no body. PATCH uses the existing UUID in the path and does not use creation idempotency semantics. There is no retained `PUT /notifications/{id}` route and no dedicated transport-update route. Unsupported methods and media types use normal framework behavior (`405` and `415` respectively); no new error handler is part of this change.

Writes use `Content-Type: application/json`; successes use `application/json`. Existing `400`, `404` and `409` errors retain the `application/problem+json` contract and existing trace-ID behavior. Bean-validation failures retain the field-error map, using nested paths such as `notification.transport.mode`. Unexpected persistence failures retain existing server-error behavior; they must not become successful saves.

### Request body for both POST and PATCH

```json
{
  "organisationId": "importer",
  "notification": {
    "transport": {
      "mode": "AIR_FREIGHT"
    }
  }
}
```

Only those fields are accepted at their respective levels:

- `organisationId`: required, a non-blank JSON string. On PATCH it is an ownership assertion, never an editable field.
- `notification`: required, a non-null object.
- `notification.transport`: required, a non-null object.
- `notification.transport.mode`: required, a non-null string equal to one of the three canonical values, case-sensitive.

Reject unknown fields at every level, including legacy `transportMethod`, arrival on POST (PATCH accepts it; see [Arrival details](#arrival-details)), document-derived transport fields, provenance, server-owned identity/state/timestamps and private persistence metadata. Preserve existing rejection of malformed JSON, trailing content, non-string organisation IDs, numeric enum values and invalid UUIDs. Requests cannot create an empty draft or clear the mode.

PATCH is a documented partial-update contract, **not RFC JSON Merge Patch**. Omitted fields are preserved recursively; explicit nulls and empty/no-mode patches are rejected in this initial phase. Future field writers require their own agreed validation and update semantics; adding them to the aggregate schema alone does not authorise them.

POST and PATCH consume `application/json`; `application/merge-patch+json` is not accepted.

### First-save example

```http
POST /notifications
Content-Type: application/json
Idempotency-Key: 8d2804a9-4bf1-4e31-b89a-bf609df32873

{"organisationId":"importer","notification":{"transport":{"mode":"AIR_FREIGHT"}}}
```

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /notifications/8d2804a9-4bf1-4e31-b89a-bf609df32873
```

```json
{
  "id": "8d2804a9-4bf1-4e31-b89a-bf609df32873",
  "reference": "GBN-FIS-26-A7K2Q",
  "organisationId": "importer",
  "status": "DRAFT",
  "createdAt": "2026-11-23T20:00:00.123Z",
  "updatedAt": "2026-11-23T20:00:00.123Z",
  "notification": {
    "transport": {
      "mode": "AIR_FREIGHT"
    }
  }
}
```

The server owns the reference, state and timestamps. Dates are UTC ISO 8601 strings with the existing millisecond precision. Absent optional fields are omitted, not emitted as null placeholders. No extra response wrapper is added.

### Patch and read example

```http
PATCH /notifications/8d2804a9-4bf1-4e31-b89a-bf609df32873
Content-Type: application/json

{"organisationId":"importer","notification":{"transport":{"mode":"ROAD_VEHICLE"}}}
```

PATCH returns `200 OK` with:

```json
{
  "id": "8d2804a9-4bf1-4e31-b89a-bf609df32873",
  "reference": "GBN-FIS-26-A7K2Q",
  "organisationId": "importer",
  "status": "DRAFT",
  "createdAt": "2026-11-23T20:00:00.123Z",
  "updatedAt": "2026-11-23T20:05:00.456Z",
  "notification": {
    "transport": {
      "mode": "ROAD_VEHICLE"
    }
  }
}
```

`GET /notifications/8d2804a9-4bf1-4e31-b89a-bf609df32873` returns `200 OK` with this current representation, including any other stored schema-defined sections and fields.

Repeating the original POST returns `200 OK`, the same Location and the **first-save response above**, including `AIR_FREIGHT` and `updatedAt` equal to `createdAt`. It does not revert the stored `ROAD_VEHICLE`. Reusing the key with `ROAD_VEHICLE` or another organisation returns `409`, because replay comparison uses the original request, not current content.

## Persistence and mapping

The [aggregate schema](../src/main/resources/schemas/fis-notification-aggregate.schema.json) describes canonical JSON. It is not a literal BSON collection schema:

- MongoDB retains the journey UUID as string `_id`; canonical JSON exposes `id`.
- Persisted timestamps remain BSON dates; API timestamps are UTC strings.
- Spring Data mapping metadata and private creation-request metadata are not schema-defined JSON properties.
- Do not apply the JSON schema directly as a MongoDB collection validator.

Request and response DTOs define the API boundary. Boundary mapping accepts only the supported request subset and projects the full canonical response; the service owns creation and update rules, and Spring Data owns identifier/date storage conversion. Do not serialize the persistence entity directly or blindly expose the raw BSON record. API-local serialization must omit absent optional fields without changing unrelated endpoints.

The envelope and transport have explicit fields. Other schema-defined sections and provenance are retained as read-only value maps, preserving nested values, arrays and explicit null provenance values without implementing future section writers or validation rules. The request DTOs do not accept those maps. Optional envelope metadata is retained when already stored; this change does not generate concurrency tokens, submission timestamps or creator details.

Retain the originally accepted organisation ID and nested transport mode as immutable private creation-request metadata in the same MongoDB record, outside `notification` content. Retain the accepted values, not only a hash or the current mode. The response projection must exclude this metadata, `_id`, `_class` and any legacy flat transport field.

Reconstruct replay JSON from that original request, immutable ID/reference/createdAt and the existing draft-state guard. It contains only the content present at creation and uses `createdAt` for both timestamps. Never write the reconstructed response back. A replay does not allocate another reference, change timestamps or count as another creation.

## Update and draft invariants

- PATCH retains the existing atomic guard on ID, matching organisation and `DRAFT` state, with primary-read behavior and no upsert.
- Its only write targets are `notification.transport.mode` and `updatedAt`; use a dotted-field update, not replacement of `notification` or `notification.transport`.
- Preserve ID, reference, organisation, createdAt, original creation-request metadata, every omitted notification section, all other transport fields and all provenance.
- Saving the same mode still returns `200` and refreshes updatedAt, as the existing successful-save behavior does.
- A missing target returns `404`. A present target that fails ownership/state checks returns `409`. Do not recreate it or allocate a replacement identity. Classify a failed PATCH guard by record existence, without requiring deserialization of a non-draft status the current Java model does not support.
- Concurrent identical first saves still create one record/reference. Existing bounded reference-collision retry behavior remains unchanged; no new concurrency feature is added.
- A mode change does not clear vessel names, IMO numbers, flight numbers, registrations, container numbers or transport-document references, even if some become irrelevant. The aggregate schema permits their coexistence. Reconciliation is deferred.
- The supplied organisation ID is not a new authenticated authorisation mechanism. Preserve the existing assertion/guard without broadening this ticket.

Draft validity is distinct from submission readiness. The [submitted schema](../src/main/resources/schemas/fis-notification-submitted.schema.json) must not be used to reject these transport-only drafts. Request validation can require mode even though the aggregate schema allows an incomplete draft without it.

Legacy flat records have no compatibility reader or migration in this scope. Use the existing isolated test setup with fresh test data. This does not authorise deletion, silent recreation, identity replacement or a claim that historical flat records are supported by the new contract. Any action on an existing shared dataset requires separate approval.

## Acceptance criteria

1. All three canonical modes create a nested draft with `201`, the supplied UUID and Location. Identical retry returns `200` without another record/reference; conflicting retry returns `409`.
2. GET and PATCH expose full canonical JSON with `notification.transport.mode`, required identity/state/timestamps and no flat/private/BSON fields. Aggregate-schema validation with format checking succeeds for every success representation, including historical replay.
3. Invalid or unsupported fields/types/values at every request level, explicit nulls and empty patches return `400` before persistence. Nested validation paths are reported through the existing error contract.
4. PATCH preserves ID/reference/ownership/createdAt and every omitted field. A fixture containing arrival, supporting-document references, transport details and provenance survives both a mode change and a same-mode save, as observed through GET.
5. Missing patches do not upsert; wrong-organisation/non-draft patches do not mutate data. Existing `404`/`409` behavior, reference collision handling and creation metrics remain intact.
6. Create, patch, replay-original and read-current preserves the historical response/current-state distinction. Persisted original creation metadata survives reload and remains unchanged and absent from every API response.
7. The existing journey UUID works for retries and GET-before-save recovery, including changing the transport selection after a lost creation response. A known saved draft is never silently recreated when missing.
8. An incomplete transport draft passes the aggregate schema without arrival/commodity/submission details. No runtime aggregate/submission validator, arrival writer (since added by FISB-73), new lifecycle state or generated concurrency field is introduced.
9. BSON mapping retains the string `_id`, unique reference and BSON-date timestamps with millisecond precision. Canonical projection retains all present schema-defined content instead of dropping sections without writers.
10. The flat body and old PUT route are not retained; API documentation and OpenAPI annotations describe the new POST/GET/PATCH contract.

## Test-first implementation and validation

### Agreed seams

| Seam | Existing test location | Catches | Does not prove |
| --- | --- | --- | --- |
| HTTP controller contract | `NotificationControllerTest` | Binding, strict nested validation, verbs/statuses, response projection and problem details | Real persistence behavior |
| HTTP journey with MongoDB | `NotificationIT` | End-to-end backend create/read/patch/replay/recovery and omitted-field preservation | Frontend presentation/integration |
| MongoDB mapping boundary | Mapping cases in `NotificationIT` using actual Spring Data/MongoDB operations | Persisted/reloaded identity, BSON dates and private replay metadata | UI behavior or business outcomes by itself |

Use the existing test files and isolated MongoDB setup. Storage assertions belong explicitly to the mapping seam; journey assertions use HTTP responses and GET rather than inspecting internal update commands. Keep directly dependent service tests consistent, without adding more tests of internal collaborator calls.

Implement one vertical red/green slice at a time, not all tests before all code:

1. **Canonical creation:** fail an HTTP contract case for a nested transport request and canonical response; implement strict request DTOs, canonical transport mode, nested creation content and explicit response projection. Parameterize the other two modes. Add a test-scoped Draft 2020-12 schema validator with format checking, not a runtime dependency.
2. **Replay and mapping:** fail a create/reload/retry case at the MongoDB boundary; retain immutable original request metadata, identity/reference/timestamp mappings and existing collision handling. Check that replay JSON passes the aggregate schema and excludes internal fields.
3. **Incremental PATCH:** fail an HTTP + MongoDB case changing mode on a valid draft seeded with other schema-defined content; implement the guarded leaf update and return its full current representation. Verify preservation through GET and same-mode save behavior.
4. **Historical replay after PATCH:** fail a create/patch/replay/read sequence; implement response-only original reconstruction and confirm conflicting payloads do not alter current state.
5. **Strict boundary failures and recovery:** adapt focused existing cases for nested malformed/unsupported input, missing targets and ownership/state guards. Verify that a present non-draft PATCH target returns `409` without expanding the lifecycle model; use existence-based classification when the update guard fails. Retain concurrent-first-save and reference-collision cases, and verify lost-response recovery using the same UUID and GET before PATCH. Do not add concurrency machinery.
6. **Contract completion:** remove the old PUT mapping, update OpenAPI annotations and existing dependent tests, and confirm incomplete draft responses against the aggregate schema without submission checks. No broader refactoring or coverage chasing.

Run targeted unit tests first:

```bash
mvn -Dtest=NotificationControllerTest,NotificationServiceTest test
```

Run the targeted MongoDB journey and mapping checks using the existing isolated setup and compiled tests:

```bash
mvn -Dit.test=NotificationIT failsafe:integration-test failsafe:verify
```

The schema checks use `com.networknt:json-schema-validator:1.5.9` with test scope, Draft 2020-12 and format assertions enabled. Schemas are loaded from the classpath with a base location so their relative identifiers resolve locally. No runtime validator was added.

Implementation proceeded through failing creation, storage-metadata, PATCH-preservation, non-draft-guard, exact-mode and media-type tests before their corresponding changes. The controller test mapper uses Spring's ProblemDetail mixin, matching the production field-error response.

The full `mvn clean verify` quality gate passed under the repository's Corretto 25/Maven Docker build image with the existing Testcontainers setup: 93 unit tests, 31 integration tests, no failures or skips, and the coverage gate satisfied. This required no host Java/Maven installation, new environment, shared-dataset changes or deployment.

## Arrival details

FISB-73 extends `PATCH /notifications/{id}` to save the [arrival](../GLOSSARY.md): the port of entry and expected arrival. It follows the transport contract above unless stated here.

### Request

```json
{
  "organisationId": "importer",
  "notification": {
    "arrival": {
      "portOfEntry": "FELIXSTOWE",
      "expectedArrivalDate": "2027-06-15",
      "expectedArrivalTime": "14:30"
    }
  }
}
```

- PATCH accepts `notification.transport`, `notification.arrival` or both; at least one is required. Both in one request are saved in one atomic update. Omitted sections are preserved; explicit nulls and `{}` are rejected.
- POST is unchanged and remains transport-only. Arrival is not part of the creation request, so creation replay ([ADR 0002](adr/0002-idempotent-creation-replay.md)) is unaffected.
- An arrival is saved as a whole: `portOfEntry`, `expectedArrivalDate` and `expectedArrivalTime` are all required JSON strings. It cannot be cleared or partially changed.
- `portOfEntry`: an upper-case code matching `^[A-Z0-9-]+$`, such as `FELIXSTOWE`. It is not yet checked against port reference data or against the transport mode.
- `expectedArrivalDate`: exactly `YYYY-MM-DD` and a real calendar date.
- `expectedArrivalTime`: exactly `HH:mm` on the 24-hour clock, matching the aggregate schema.
- `borderControlPost` and `expectedArrivalAt` are server-owned and rejected if sent.

### Derived arrival instant

The date and time are UK local time (`Europe/London`). The server derives `expectedArrivalAt` in UTC:

- A time that does not exist because the UK clocks go forward is rejected.
- A time that occurs twice because the UK clocks go back uses the earlier instant: `2027-10-31` `01:30` is `2027-10-31T00:30:00Z`.
- An arrival before the current time is rejected. Now itself is accepted. The frontend's stricter journey rules (at least 2 hours ahead, at most 1 year ahead) are not repeated in the backend.

Clock-change and past-arrival errors are reported at `notification.arrival.expectedArrivalTime` in the existing field-error map. All arrival validation happens before persistence.

### Response and storage

The response is the full current notification representation, including:

```json
"arrival": {
  "portOfEntry": "FELIXSTOWE",
  "expectedArrivalDate": "2027-06-15",
  "expectedArrivalTime": "14:30",
  "expectedArrivalAt": "2027-06-15T13:30:00Z"
}
```

Arrival is a typed part of notification content. `expectedArrivalDate` and `expectedArrivalTime` are stored as strings because they are local values. `expectedArrivalAt` is stored as a BSON date for sorting and querying, and serialised like the other timestamps. PATCH sets `notification.arrival.portOfEntry`, `.expectedArrivalDate`, `.expectedArrivalTime`, `.expectedArrivalAt` and `updatedAt` as individual fields, so `borderControlPost` and every other field are preserved.

There is no compatibility reader or migration for arrival maps stored in other shapes; no code wrote arrival before this change.

### Known limitations

- **Port and border control post are not reconciled.** `borderControlPost` is read-only and retained when the port of entry changes, so a stored border control post can go out of date, as transport details can after a mode change.
- **A stored arrival can go out of date.** Arrival is validated only when it is in the request. A saved arrival that later passes does not block other patches and is not checked again.

### Tests

- `NotificationControllerTest`: strict arrival request validation, nested error paths and the clocks-go-forward case.
- `NotificationIT`, with a fixed test `Clock` at `2026-11-01T00:00:00Z`: arrival-only and combined patches, overwriting while preserving `borderControlPost`, past and boundary arrivals, the clocks-go-back case and BSON storage of `expectedArrivalAt`.

## Frontend dependency and release coordination

The current frontend is incompatible with this target backend contract. A separate frontend change must:

- Send canonical modes in the nested POST/PATCH body and use `PATCH /notifications/{id}` rather than PUT.
- Validate and compare the nested response mode, and consume structured notification content rather than the flat field.
- Address radio values, any translation and saved-choice preselection in its own scope; those choices are not settled or implemented here.
- Preserve the session UUID, organisation/reference checks, accepted POST `200`/`201`, and GET-before-save lost-response recovery.
- Test first save, return-to-transport selection, same-ID updates and changed-selection recovery against the new contract.

No mixed old/new frontend/backend pairing is supported, and no backend alias or dual contract will be supplied. Matching versions must be coordinated before shared use.

Backend validation uses the existing isolated tests. This ticket does not provision a new environment or perform deployments. Shared-environment rollout, traffic management, deployment ownership, rollback arrangements and any action on existing datasets require a separate team decision.

### Team confirmations still required

- Ownership and PR sequencing for the still-open transport PR #7 and the backend alignment change.
- The separate frontend change, its presentation-mapping choices, owner and compatible release version.
- Shared-environment rollout and rollback arrangements, including suitability of existing datasets without a legacy migration.
- External BTMS/CDS confirmation of the existing reference format, already identified in PR #7; this ticket does not change that format.
- Ownership of the deferred runtime aggregate validator and later submission-specific validation.
- Arrival (FISB-73), for Jason: who sets `borderControlPost`, from which reference data, and when is it reconciled after the port of entry changes?
- Arrival (FISB-73), for Jason: where should a stored arrival that is now in the past be caught, for example at submission?
- Arrival (FISB-73), for Jason: the frontend's seeded port-of-entry options are seaports only, so an air freight importer cannot choose an airport. Who owns the port and airport reference data, and when will it replace the seeded list?

The backend API shape, PATCH semantics, original replay behavior, JSON/BSON boundary and preservation rules are agreed; these team dependencies do not reopen those decisions.
