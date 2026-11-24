package uk.gov.defra.trade.imports.fish.be.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.fish.be.controller.dto.SaveNotificationRequest;
import uk.gov.defra.trade.imports.fish.be.controller.dto.NotificationResponse;
import uk.gov.defra.trade.imports.fish.be.service.NotificationService;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Create and update import notification drafts")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a draft", description =
        "Use a journey UUID as Idempotency-Key. Identical replays return 200; conflicting replays return 409.")
    @ApiResponse(responseCode = "201", description = "Draft created")
    @ApiResponse(responseCode = "200", description = "Identical creation replay")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    @Timed("controller.createNotification.time")
    public ResponseEntity<NotificationResponse> create(@RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody SaveNotificationRequest request) {
        var result = service.create(key, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
            .location(URI.create("/notifications/" + result.notification().getId()))
            .body(NotificationResponse.from(result.notification()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @Timed("controller.getNotification.time")
    public NotificationResponse findById(@PathVariable UUID id) {
        return NotificationResponse.from(service.findById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Patch draft transport mode",
        description = "Initially accepts only notification.transport.mode and an organisationId ownership assertion. "
            + "Preserves omitted fields recursively. Only DRAFT notifications can be updated.")
    @ApiResponse(responseCode = "200", description = "Draft updated")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @ApiResponse(responseCode = "409", description = "Organisation or status conflict")
    @Timed("controller.updateNotification.time")
    public NotificationResponse update(@PathVariable UUID id, @Valid @RequestBody SaveNotificationRequest request) {
        return NotificationResponse.from(service.update(id, request));
    }
}
