package com.devbridge.backend.domain.document.service;

import com.devbridge.backend.domain.document.dto.DocumentAnalysisRequest;
import com.devbridge.backend.domain.document.dto.DocumentAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAnalysisClient {

    private final ObjectMapper objectMapper;

    @Value("${devbridge.ai-engine.base-url:http://127.0.0.1:8010}")
    private String aiEngineBaseUrl;

    @Value("${devbridge.ai-engine.internal-api-key:changeme}")
    private String internalApiKey;

    @Value("${devbridge.ai-engine.document-analysis-path:/api/ingestion/document}")
    private String documentAnalysisPath;

    @Value("${devbridge.ai-engine.timeout-seconds:30}")
    private long timeoutSeconds;

    public DocumentAnalysisResponse analyze(DocumentAnalysisRequest request) {
        String url = buildAnalysisUrl();
        String requestBody = serializeRequest(request);

        log.info("Document analysis request to {}: body={}", url, requestBody);

        WebClient webClient = WebClient.builder()
                .baseUrl(aiEngineBaseUrl)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();

        String responseBody = webClient.post()
                .uri(documentAnalysisPath)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.warn("AI Engine document analysis failed. status={}, body={}, url={}",
                                            clientResponse.statusCode(), body, url);
                                    return reactor.core.publisher.Mono.error(
                                            new IllegalStateException(
                                                    "AI Engine document analysis failed. status="
                                                            + clientResponse.statusCode()
                                                            + ", body=" + body));
                                })
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        try {
            return objectMapper.readValue(responseBody, DocumentAnalysisResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize AI Engine response: " + responseBody, e);
        }
    }

    private String serializeRequest(DocumentAnalysisRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AI Engine request.", e);
        }
    }

    private String buildAnalysisUrl() {
        String normalizedBaseUrl = aiEngineBaseUrl.endsWith("/")
                ? aiEngineBaseUrl.substring(0, aiEngineBaseUrl.length() - 1)
                : aiEngineBaseUrl;

        String normalizedPath = documentAnalysisPath.startsWith("/")
                ? documentAnalysisPath
                : "/" + documentAnalysisPath;

        return normalizedBaseUrl + normalizedPath;
    }
}
