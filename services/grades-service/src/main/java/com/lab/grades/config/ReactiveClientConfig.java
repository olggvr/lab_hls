package com.lab.grades.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ReactiveClientConfig {
    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @Qualifier("assignmentCircuitBreaker")
    ReactiveCircuitBreaker assignmentCircuitBreaker(ReactiveCircuitBreakerFactory<?, ?> factory) {
        return factory.create("assignmentService");
    }

    @Bean
    @Qualifier("messageCircuitBreaker")
    ReactiveCircuitBreaker messageCircuitBreaker(ReactiveCircuitBreakerFactory<?, ?> factory) {
        return factory.create("messageService");
    }
}
