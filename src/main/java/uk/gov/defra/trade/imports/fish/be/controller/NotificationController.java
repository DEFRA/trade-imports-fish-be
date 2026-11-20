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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.fish.be.controller.dto.SaveNotificationRequest;
import uk.gov.defra.trade.imports.fish.be.domain.Notification;
import uk.gov.defra.trade.imports.fish.be.service.NotificationService;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Create and update import notification drafts")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @PostMapping
    @Operation(summary = "Create a draft", description =
        "Use a journey UUID as Idempotency-Key. Identical replays return 200; conflicting replays return 409.")
    @ApiResponse(responseCode = "201", description = "Draft created")
    @ApiResponse(responseCode = "200", description = "Identical creation replay")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    @Timed("controller.createNotification.time")
    public ResponseEntity<Notification> create(@RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody SaveNotificationRequest request) {
        var result = service.create(key, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
            .location(URI.create("/notifications/" + result.notification().getId()))
            .body(result.notification());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @Timed("controller.getNotification.time")
    public Notification findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update draft transport",
        description = "Preserves identity, reference and organisation. Only DRAFT notifications can be updated.")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @ApiResponse(responseCode = "409", description = "Organisation or status conflict")
    @Timed("controller.updateNotification.time")
    public Notification update(@PathVariable UUID id, @Valid @RequestBody SaveNotificationRequest request) {
        return service.update(id, request);
    }
}
