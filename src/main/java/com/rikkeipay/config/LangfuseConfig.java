package com.rikkeipay.config;

import io.langfuse.client.LangfuseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration for initializing LangfuseClient securely.
 * Replaces hardcoded credentials with dynamic configuration via LangfuseProperties.
 */
@Configuration
public class LangfuseConfig {

    private static final Logger log = LoggerFactory.getLogger(LangfuseConfig.class);

    private final LangfuseProperties properties;

    public LangfuseConfig(LangfuseProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "langfuse", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LangfuseClient langfuseClient() {
        log.info("Initializing LangfuseClient connecting to: {}, environment: {}", 
                 properties.getBaseUrl(), properties.getEnvironment());
        
        return new LangfuseClient(
            properties.getPublicKey(),
            properties.getSecretKey(),
            properties.getBaseUrl()
        );
    }
}
