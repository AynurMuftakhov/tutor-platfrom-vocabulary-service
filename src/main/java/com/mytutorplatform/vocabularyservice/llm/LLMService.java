package com.mytutorplatform.vocabularyservice.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytutorplatform.vocabularyservice.model.entity.PromtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class LLMService {
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public PromtResponse callLlm(String word) {
        String json = chatModel
                .call(buildPrompt(word));

        return parseJson(json);
    }

    private PromtResponse parseJson(String json) {
        try {
            return objectMapper.readValue(json, PromtResponse.class);
        } catch (IOException e) {
            return new PromtResponse();
        }
    }

    private String buildPrompt(String word) {
        return """
            You are an English lexicographer. Reply in valid JSON:
            {
              "definition": "<clear, concise English definition (≤25 words)>",
              "synonyms": ["<up to 8 close synonyms>"],
              "translationRu": "<Russian translation (1-3 words)>",
              "partOfSpeech": "<noun | verb | adjective | adverb>",
              "phonetic": "<IPA or Arpabet transcription>",
              "difficulty": "<integer 1–5 where 1=beginner can learn easily, 5=very hard to learn>",
              "popularity": "<integer 1–5 where 1=rare, 5=very common>",
              "exampleSentence": "<simple CEFR B1-B2 level example sentence>"
            }
            Word: "%s"
            """.formatted(word);
    }
}
