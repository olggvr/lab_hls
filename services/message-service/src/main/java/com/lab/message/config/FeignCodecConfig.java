package com.lab.message.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * OpenFeign is blocking and uses Spring's HttpMessageConverters even when the
 * hosting application is WebFlux. Register only the JSON codec it needs;
 * adding the MVC starter would change the application's web stack.
 */
@Configuration
public class FeignCodecConfig {
    @Bean
    HttpMessageConverters feignMessageConverters(ObjectMapper objectMapper) {
        return new HttpMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper));
    }
}
