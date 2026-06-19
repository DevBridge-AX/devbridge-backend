package com.devbridge.backend.global.config.fastapi;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final FastApiProperties fastApiProperties;

    @Bean
    public WebClient fastApiWebClient() {
        return WebClient.builder()
                .baseUrl(fastApiProperties.getBaseUrl())
                .defaultHeader("X-Internal-Api-Key", fastApiProperties.getInternalApiKey())
                .build();
    }
}
