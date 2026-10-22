package com.lab.message.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "academic-service", path = "/internal/directory", fallback = AcademicClientFallback.class)
public interface AcademicClient {
    @PostMapping("/direct")
    AudienceResponse resolveDirect(@RequestBody DirectAudienceRequest request);

    @PostMapping("/broadcast")
    AudienceResponse resolveBroadcast(@RequestBody BroadcastAudienceRequest request);
}
