package com.lab.message.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", path = "/internal/directory", fallback = UserClientFallback.class)
public interface UserClient {
    @PostMapping("/direct")
    AudienceResponse resolveDirect(@RequestBody DirectAudienceRequest request);

    @PostMapping("/broadcast")
    AudienceResponse resolveBroadcast(@RequestBody BroadcastAudienceRequest request);
}
