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
                You are an English lexicographer and CEFR vocabulary grader.
               Reply in valid JSON with the exact keys below and no extra keys.
    
              IMPORTANT: "difficulty" MUST represent the CEFR level of the WORD (how advanced it is for learners), NOT how easy it is to explain or memorize.
    
              Use this mapping:
              1 = A1–A2 (basic everyday vocabulary)
              2 = B1 (intermediate, common general vocabulary)
              3 = B2 (upper-intermediate, more precise/abstract, less frequent)
              4 = C1 (advanced, academic/professional, nuanced, low-frequency in everyday speech)
              5 = C2 (proficiency, rare/technical/literary/idiomatic or highly nuanced)
    
              Grading criteria (choose the best fit):
              - Frequency in everyday conversation (higher frequency => lower difficulty)
              - Abstractness and nuance (more abstract/nuanced => higher difficulty)
              - Register (informal/common => lower; formal/academic/legal/technical => higher)
              - Typical appearance in B2/C1/C2 texts (academic papers, policy, literature)
              - Polysemy and collocational constraints (more constraints => higher difficulty)
    
              Also:
              - "popularity" is general frequency (1 rare – 5 common). It can differ from "difficulty" but usually correlates.
              - The exampleSentence simplicity MUST NOT affect difficulty grading.
    
              Return:
              {
                "definition": "<clear, concise English definition (≤25 words)>",
                "synonyms": ["<up to 8 close synonyms>"],
                "translationRu": "<Russian translation (1-3 words)>",
                "partOfSpeech": "<noun | verb | adjective | adverb>",
                "phonetic": "<IPA transcription>",
                "difficulty": "<integer 1–5 using the CEFR mapping above>",
                "popularity": "<integer 1–5 where 1=rare, 5=very common>",
                "exampleSentence": "<CEFR B1-B2 level example sentence using the word naturally>"
              }
    
              Word: "%s"
            """.formatted(word);
    }
}
