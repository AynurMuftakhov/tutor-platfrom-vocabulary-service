package com.mytutorplatform.vocabularyservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class TextToSpeechService {
  private final WebClient client;

  private final String voiceId;

  public TextToSpeechService(
          @Value("${elevenlabs.api-key}") String apiKey,
          @Value("${elevenlabs.voice-id}") String voiceId
  ) {
    this.client = WebClient.builder()
            .baseUrl("https://api.elevenlabs.io")
            .defaultHeader("xi-api-key", apiKey)
            .build();
    this.voiceId = voiceId;
  }

  public byte[] generateSpeech(String text) {
    // Choose a model and a stable output format:
    // mp3_44100_128 is a good default; you can also try turbo/flash models.
    return client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/v1/text-to-speech/{voiceId}")
                    .queryParam("output_format", "mp3_44100_128")
                    .build(voiceId))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.valueOf("audio/mpeg"))
            .bodyValue(Map.of(
                    "text", text + ".",
                    "model_id", "eleven_multilingual_v2", // or "eleven_turbo_v2_5" for speed/cost
                    "voice_settings", Map.of(
                            "stability", 0.75,
                            "similarity_boost", 0.8
                    )
                    // Optional: "seed", e.g., 42, to reduce run-to-run variation.
            ))
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
  }
}
