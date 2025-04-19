package com.mytutorplatform.vocabularyservice.service;

import com.mytutorplatform.vocabularyservice.mapper.VocabularyMapper;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordResponse;
import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import com.mytutorplatform.vocabularyservice.repository.VocabularyWordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyWordRepository wordRepo;
    private final VocabularyMapper mapper;

    public VocabularyWordResponse createWord(VocabularyWordRequest request) {
        VocabularyWord word = mapper.toEntity(request);

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
}
