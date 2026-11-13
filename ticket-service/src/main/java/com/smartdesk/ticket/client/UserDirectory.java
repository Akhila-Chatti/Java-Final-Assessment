package com.smartdesk.ticket.client;

import com.smartdesk.ticket.dto.client.UserContact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Name and email of a requester or agent, looked up in the auth service instead of being copied into ticket tables. */
@Component
@Slf4j
public class UserDirectory {

    private final UserClient client;
    private final String internalKey;

    public UserDirectory(UserClient client, @Value("${smartdesk.internal-key}") String internalKey) {
        this.client = client;
        this.internalKey = internalKey;
    }

    /** null when the auth service is down or the user is unknown; never cached, so the next call retries. */
    // ponytail: in-memory cache, never expires; a renamed user shows the old name until restart. Add a TTL if that matters.
    @Cacheable(cacheNames = "users", unless = "#result == null")
    public UserContact find(UUID id) {
        try {
            return client.get(internalKey, id);
        } catch (RuntimeException e) {
            log.warn("User lookup failed for {}: {}", id, e.toString());
            return null;
        }
    }
}
