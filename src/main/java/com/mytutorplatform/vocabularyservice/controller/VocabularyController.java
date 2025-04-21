package com.mytutorplatform.vocabularyservice.controller;

import com.mytutorplatform.vocabularyservice.model.dto.CreateWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordResponse;
import com.mytutorplatform.vocabularyservice.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.mytutorplatform.vocabularyservice.constants.GenericConstants.VOCABULARY_WORDS_PATH;
import static com.mytutorplatform.vocabularyservice.constants.GenericConstants.API_V_1;


@RestController
@RequestMapping(API_V_1 + VOCABULARY_WORDS_PATH)
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService service;

    @PostMapping
    public ResponseEntity<VocabularyWordResponse> save(@RequestBody @Valid VocabularyWordRequest request) {
        VocabularyWordResponse word = service.saveWord(request);

        URI location = URI.create(API_V_1 + VOCABULARY_WORDS_PATH + "/" + word.getId());

        return ResponseEntity.created(location).body(word);
    }

    @PostMapping("/create")
    public ResponseEntity<VocabularyWordResponse> create(@RequestBody CreateWordRequest request) {
        VocabularyWordResponse word = service.createWord(request);
        
        URI location = URI.create(API_V_1 + VOCABULARY_WORDS_PATH + "/" + word.getId());

        return ResponseEntity.created(location).body(word);
    }

    @GetMapping
    public List<VocabularyWordResponse> getAll() {
        return service.getAllWords();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VocabularyWordResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getWordById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VocabularyWordResponse> update(@PathVariable UUID id,
                                         @RequestBody VocabularyWordRequest request) {
        return ResponseEntity.ok(service.updateWord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteWord(id);

        return ResponseEntity.noContent().build();
    }
}