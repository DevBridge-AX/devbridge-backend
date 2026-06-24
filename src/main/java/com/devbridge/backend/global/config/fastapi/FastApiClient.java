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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient fastApiWebClient;

    public Flux<ServerSentEvent<String>> streamChat(FastApiChatRequest request) {
        return fastApiWebClient.post()
                .uri("/api/chat/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});
    }

    public Mono<Void> ingestDocument(Map<String, Object> request) {
        return fastApiWebClient.post()
                .uri("/api/ingestion/document")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(ingestionRetrySpec())
                .then();
    }

    public Mono<Void> ingestGit(Map<String, Object> request) {
        return fastApiWebClient.post()
                .uri("/api/ingestion/git")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(ingestionRetrySpec())
                .then();
    }

    public Mono<Void> ingestOwnerAnswer(Map<String, Object> request) {
        return fastApiWebClient.post()
                .uri("/api/ingestion/owner-answer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(ingestionRetrySpec())
                .then();
    }

    private Retry ingestionRetrySpec() {
        return Retry.backoff(3, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(10))
                .filter(ex -> !(ex instanceof WebClientResponseException.BadRequest))
                .doBeforeRetry(signal ->
                        log.warn("인덱싱 요청 재시도 ({}/3): {}", signal.totalRetries() + 1, signal.failure().getMessage()));
    }

    public Mono<UsageSummaryResponse> getUsageSummary(String workspaceId) {
        return fastApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/usage/summary")
                        .queryParam("workspace_id", workspaceId)
                        .build())
                .retrieve()
                .bodyToMono(UsageSummaryResponse.class);
    }
}
