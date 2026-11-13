package com.smartdesk.ticket.client;

import com.smartdesk.ticket.dto.client.UserContact;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/** Users live in the gateway's auth DB. Always call it through UserDirectory (cached). */
@FeignClient(name = "api-gateway")
public interface UserClient {

    @GetMapping("/internal/users/{id}")
    UserContact get(@RequestHeader("X-Internal-Key") String key, @PathVariable UUID id);
}
