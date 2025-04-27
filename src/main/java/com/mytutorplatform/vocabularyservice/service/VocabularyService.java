package com.mytutorplatform.vocabularyservice.service;

import com.mytutorplatform.vocabularyservice.llm.LLMService;
import com.mytutorplatform.vocabularyservice.mapper.VocabularyMapper;
import com.mytutorplatform.vocabularyservice.model.dto.CreateWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordResponse;
import com.mytutorplatform.vocabularyservice.model.entity.PromtResponse;
import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import com.mytutorplatform.vocabularyservice.repository.VocabularyWordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyService {
    private final VocabularyWordRepository wordRepo;
    private final VocabularyMapper mapper;
    private final LLMService lLMService;

    public VocabularyWordResponse saveWord(VocabularyWordRequest request) {
        VocabularyWord word = mapper.toEntity(request);

        return mapper.toResponse(wordRepo.save(word));
    }

    public VocabularyWordResponse createWord(CreateWordRequest request) {
        String text = request.getText();
        PromtResponse response = lLMService.callLlm(text);
        if (!isValidResponse(response)) {
            log.error("Invalid LLM response {}", response);
            return null;
        }

        String audioUrl = null;

        VocabularyWord word = VocabularyWord.builder()
                .text(text)
                .translation(response.getTranslationRu())
                .partOfSpeech(response.getPartOfSpeech())
                .definitionEn(response.getDefinition())
                .synonymsEn(response.getSynonyms())
                .phonetic(response.getPhonetic())
                .audioUrl(audioUrl)
                .difficulty(response.getDifficulty())
                .popularity(response.getPopularity())
                .exampleSentence(response.getExampleSentence())
                .createdByTeacherId(request.getTeacherId())
                .editedAt(null)
                .build();

        log.debug("Word generated with id {}", word);

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

    private boolean isValidResponse(PromtResponse response) {
        return response.getDefinition() != null && response.getTranslationRu() != null;
    }
}
