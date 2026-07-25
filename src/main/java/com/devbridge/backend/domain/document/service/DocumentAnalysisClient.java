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

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAnalysisClient {

    private final ObjectMapper objectMapper;
    private final WebClient fastApiWebClient;

    @Value("${devbridge.ai-engine.document-analysis-path:/api/analysis/document}")
    private String documentAnalysisPath;

    public DocumentAnalysisResponse analyze(DocumentAnalysisRequest request) {
        String requestBody = serializeRequest(request);

        log.info("Document analysis request to {}: body={}", documentAnalysisPath, requestBody);

        String responseBody = fastApiWebClient.post()
                .uri(documentAnalysisPath)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.warn("AI Engine document analysis failed. status={}, body={}, path={}",
                                            clientResponse.statusCode(), body, documentAnalysisPath);
                                    return reactor.core.publisher.Mono.error(
                                            new IllegalStateException(
                                                    "AI Engine document analysis failed. status="
                                                            + clientResponse.statusCode()
                                                            + ", body=" + body));
                                })
                )
                .bodyToMono(String.class)
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
}
