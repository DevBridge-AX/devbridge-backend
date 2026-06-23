package com.devbridge.backend.domain.document.service;

import com.devbridge.backend.domain.document.dto.DocumentAnalysisRequest;
import com.devbridge.backend.domain.document.dto.DocumentAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public DocumentAnalysisResponse analyze(DocumentAnalysisRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(buildAnalysisUrl()))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI Engine document analysis failed. status="
                                + response.statusCode()
                                + ", body="
                                + response.body()
                );
            }

            return objectMapper.readValue(response.body(), DocumentAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI Engine request.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call AI Engine document analysis API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI Engine document analysis request was interrupted.", e);
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