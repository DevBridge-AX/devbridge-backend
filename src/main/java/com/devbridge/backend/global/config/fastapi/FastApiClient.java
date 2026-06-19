package com.devbridge.backend.global.config.fastapi;

import com.devbridge.backend.domain.chat.dto.fastapi.FastApiChatRequest;
import com.devbridge.backend.domain.chat.dto.fastapi.UsageSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient fastApiWebClient;

    public Flux<ServerSentEvent<String>> streamChat(FastApiChatRequest request) {
        return fastApiWebClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});
    }

    public Mono<Void> ingestDocument(Map<String, Object> request) {
        return fastApiWebClient.post()
                .uri("/ingestion/document")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    public Mono<Void> ingestGit(Map<String, Object> request) {
        return fastApiWebClient.post()
                .uri("/ingestion/git")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    public Mono<UsageSummaryResponse> getUsageSummary(String workspaceId) {
        return fastApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/usage/summary")
                        .queryParam("workspace_id", workspaceId)
                        .build())
                .retrieve()
                .bodyToMono(UsageSummaryResponse.class);
    }
}
