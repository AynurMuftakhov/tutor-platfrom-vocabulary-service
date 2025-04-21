package com.mytutorplatform.vocabularyservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytutorplatform.vocabularyservice.mapper.VocabularyMapper;
import com.mytutorplatform.vocabularyservice.model.dto.CreateWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordResponse;
import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import com.mytutorplatform.vocabularyservice.repository.VocabularyWordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyWordRepository wordRepo;
    private final VocabularyMapper mapper;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public VocabularyWordResponse saveWord(VocabularyWordRequest request) {
        VocabularyWord word = mapper.toEntity(request);

        return mapper.toResponse(wordRepo.save(word));
    }

    public VocabularyWordResponse createWord(CreateWordRequest request) {
        String text = request.getText();
        JsonNode payload = callLlm(text);
        if (!isValidLlm(payload)) {
            log.error("Invalid LLM text {}", payload);
           return null;
        }

        String audioUrl = null;

        VocabularyWord word = VocabularyWord.builder()
                .id(UUID.randomUUID())
                .text(text)
                .translation(payload.path("translation_ru").asText())
                .partOfSpeech(payload.path("part_of_speech").asText(null))
                .definitionEn(payload.path("definition").asText(null))
                .synonymsEn(objectMapper.convertValue(
                        payload.path("synonyms"), String[].class))
                .phonetic(payload.path("phonetic").asText(null))
                .audioUrl(audioUrl)
                .rawJson(payload.toString())
                .createdByTeacherId(request.getTeacherId())
                .editedAt(null)
                .build();

        return mapper.toResponse(wordRepo.save(word));
    }

    public List<VocabularyWordResponse> getAllWords() {
        return mapper.toListOfVocabularyWordResponses(wordRepo.findAll());
    }

    public VocabularyWordResponse getWordById(UUID id) {
        return mapper.toResponse(wordRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Word not found")));
    }

    public VocabularyWordResponse updateWord(UUID id, VocabularyWordRequest request) {
        VocabularyWord word = wordRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found"));

        Optional.ofNullable(request.getText()).ifPresent(word::setText);
        Optional.ofNullable(request.getTranslation()).ifPresent(word::setTranslation);
        Optional.ofNullable(request.getPartOfSpeech()).ifPresent(word::setPartOfSpeech);

        return mapper.toResponse(wordRepo.save(word));
    }

    public void deleteWord(UUID id) {
        wordRepo.deleteById(id);
    }

    private JsonNode callLlm(String word) {
        String json = chatModel
                .call(buildPrompt(word));

        return parseJson(json);
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            return objectMapper.createObjectNode();
        }
    }

    private String buildPrompt(String w) {
        return """
            You are an English lexicographer. Reply in valid JSON:
            {
              "definition": "<max 25 words>",
              "synonyms": ["≤8 items"],
              "translation_ru": "<1‑3 words>",
              "part_of_speech": "<noun|verb|adj|adv>",
              "phonetic": "<IPA or Arpabet>"
            }
            Word: "%s"
            """.formatted(w);
    }

    private boolean isValidLlm(JsonNode n) {
        return n.hasNonNull("definition")
                && n.hasNonNull("translation_ru");
    }
}
