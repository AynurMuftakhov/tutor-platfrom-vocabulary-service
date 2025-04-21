package com.mytutorplatform.vocabularyservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

/*    @Bean
    public WebClient freeDictionaryClient(@Value("${services.free-dictionary}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public WebClient datamuseClient(@Value("${services.datamuse}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public WebClient piperClient(@Value("${services.piper}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }*/
}
