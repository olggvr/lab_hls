package com.lab.message.integration;

import com.lab.message.exception.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public AudienceResponse resolveDirect(DirectAudienceRequest request) {
        throw unavailable();
    }

    @Override
    public AudienceResponse resolveBroadcast(BroadcastAudienceRequest request) {
        throw unavailable();
    }

    private DownstreamUnavailableException unavailable() {
        return new DownstreamUnavailableException("User Service временно недоступен");
    }
}
