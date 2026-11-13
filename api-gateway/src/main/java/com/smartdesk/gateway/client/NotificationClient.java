package com.smartdesk.gateway.client;

import com.smartdesk.gateway.entity.AppUser;
import com.smartdesk.gateway.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/** Asks the Notification Service (found through Eureka) to email the OTP. */
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final LoadBalancerClient loadBalancer;
    private final RestClient http = RestClient.create();

    public void sendOtp(AppUser user, String code) {
        ServiceInstance instance = loadBalancer.choose("notification-service");
        if (instance == null) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Notification service is not registered yet");
        http.post()
                .uri(instance.getUri().resolve("/api/v1/notifications/send"))
                .body(Map.of(
                        "eventId", UUID.randomUUID(),
                        "eventType", "OTP",
                        "recipientUserId", user.getUserId(),
                        "recipientEmail", user.getEmail(),
                        "params", Map.of("name", user.getName(), "otp", code)))
                .retrieve()
                .toBodilessEntity();
    }
}
