package com.smartdesk.notification.controller;

import com.smartdesk.notification.dto.NotificationResponse;
import com.smartdesk.notification.dto.SendNotificationRequest;
import com.smartdesk.notification.mapper.NotificationMapper;
import com.smartdesk.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    /** Internal: called by the gateway (OTP) and the Ticket Service; not routed publicly. */
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void send(@Valid @RequestBody SendNotificationRequest request) {
        service.accept(request);
    }

    /** The logged-in user's notifications (identity header set by the gateway). */
    @GetMapping("/me")
    public Page<NotificationResponse> mine(@RequestHeader("X-User-Id") UUID userId, Pageable page) {
        return service.mine(userId, page).map(NotificationMapper::toResponse);
    }
}
