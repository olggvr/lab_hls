package com.lab.message.integration;

import com.lab.message.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class AcademicClientFallback implements AcademicClient {
    @Override
    public AudienceResponse resolveDirect(DirectAudienceRequest request) {
        throw unavailable();
    }

    @Override
    public AudienceResponse resolveBroadcast(BroadcastAudienceRequest request) {
        throw unavailable();
    }

    private DownstreamUnavailableException unavailable() {
        return new DownstreamUnavailableException("Академический сервис временно недоступен");
    }
}
