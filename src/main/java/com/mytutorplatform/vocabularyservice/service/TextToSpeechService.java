package com.mytutorplatform.vocabularyservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TextToSpeechService {
    @Value("${elevenlabs.api-key}")
    private String apiKey;

    @Value("${elevenlabs.voice-id}")
    private String voiceId;

    public byte[] generateSpeech(String text) {
        WebClient client = WebClient.create("https://api.elevenlabs.io/v1/text-to-speech/" + voiceId);
        return client.post()
                .header("xi-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}
