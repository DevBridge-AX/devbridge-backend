package com.devbridge.backend.global.config.fastapi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "devbridge.ai-engine")
public class FastApiProperties {

    private String baseUrl;
    private String internalApiKey;
    private String documentAnalysisPath;
    private long timeoutSeconds;
    private Map<String, CreditRate> creditRates;

    @Getter
    @Setter
    public static class CreditRate {
        private BigDecimal input;
        private BigDecimal output;
    }
}
