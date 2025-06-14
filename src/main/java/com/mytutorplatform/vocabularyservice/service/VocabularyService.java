package com.mytutorplatform.vocabularyservice.service;

import com.mytutorplatform.vocabularyservice.llm.LLMService;
import com.mytutorplatform.vocabularyservice.mapper.VocabularyMapper;
import com.mytutorplatform.vocabularyservice.model.AudioPart;
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
    public static final String EXAMPLE_SENTENCE = "example_sentence_";

    private final VocabularyWordRepository wordRepo;
    private final VocabularyMapper mapper;
    private final LLMService lLMService;
    private final AudioStorageService audioStorageService;
    private final TextToSpeechService textToSpeechService;

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

        String audioUrl = generateAudio(text);
        String exampleSentenceAudioUrl = generateAudio(EXAMPLE_SENTENCE + text, response.getExampleSentence());

        VocabularyWord word = VocabularyWord.builder()
                .text(text)
                .translation(response.getTranslationRu())
                .partOfSpeech(response.getPartOfSpeech())
                .definitionEn(response.getDefinition())
                .synonymsEn(response.getSynonyms())
                .phonetic(response.getPhonetic())
                .audioUrl(audioUrl)
                .exampleSentenceAudioUrl(exampleSentenceAudioUrl)
                .difficulty(response.getDifficulty())
                .popularity(response.getPopularity())
                .exampleSentence(response.getExampleSentence())
                .createdByTeacherId(request.getTeacherId())
                .editedAt(null)
                .build();

        log.debug("Word generated with id {}", word);

        return mapper.toResponse(wordRepo.save(word));
    }

    private String generateAudio(String text) {
        return generateAudio(text, text);
    }

    private String generateAudio(String fileName, String text){
        try {
            byte[] audio = textToSpeechService.generateSpeech(text);
            return audioStorageService.saveAudioToFile(audio, fileName);
        } catch (Exception e) {
            log.error("Audio generation failed", e);
            return null;
        }
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
            Optional.ofNullable(request.getDefinitionEn()).ifPresent(word::setDefinitionEn);
            Optional.ofNullable(request.getSynonymsEn()).ifPresent(word::setSynonymsEn);
            Optional.ofNullable(request.getDifficulty()).ifPresent(word::setDifficulty);
            Optional.ofNullable(request.getPopularity()).ifPresent(word::setPopularity);
            Optional.ofNullable(request.getExampleSentence()).ifPresent(word::setExampleSentence);

        return mapper.toResponse(wordRepo.save(word));
    }

    public void deleteWord(UUID id) {
        try {
            wordRepo.deleteById(id);
        }catch (Exception e){
            log.error("Word deletion failed", e);
            throw new RuntimeException("Word deletion failed");
        }

    }

    public VocabularyWordResponse regenerateAudio(UUID id, AudioPart part) {
        VocabularyWord word = wordRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found"));

        switch (part) {
            case TEXT:
                String audioUrl = generateAudio(word.getText());
                word.setAudioUrl(audioUrl);
                break;
            case EXAMPLE_SENTENCE:
                String exampleSentenceAudioUrl = generateAudio(EXAMPLE_SENTENCE + word.getText(), word.getExampleSentence());
                word.setExampleSentenceAudioUrl(exampleSentenceAudioUrl);
                break;
            default:
                throw new RuntimeException("Invalid audio part: " + part);
        }

        return mapper.toResponse(wordRepo.save(word));
    }

    private boolean isValidResponse(PromtResponse response) {
        return response.getDefinition() != null && response.getTranslationRu() != null;
    }
}
