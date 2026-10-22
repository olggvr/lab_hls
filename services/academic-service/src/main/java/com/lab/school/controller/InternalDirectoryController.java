package com.lab.school.controller;

import com.lab.school.integration.AudienceResponse;
import com.lab.school.integration.BroadcastAudienceRequest;
import com.lab.school.integration.DirectAudienceRequest;
import com.lab.school.service.ReactiveDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/directory")
public class InternalDirectoryController {
    private final ReactiveDirectoryService directoryService;

    @PostMapping("/direct")
    public Mono<AudienceResponse> resolveDirect(@RequestBody DirectAudienceRequest request) {
        return directoryService.resolveDirect(request);
    }

    @PostMapping("/broadcast")
    public Mono<AudienceResponse> resolveBroadcast(@RequestBody BroadcastAudienceRequest request) {
        return directoryService.resolveBroadcast(request);
    }
}
