package com.mytutorplatform.vocabularyservice.controller;

import com.mytutorplatform.vocabularyservice.model.AudioPart;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateRequest;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateResponse;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateJobResponse;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateJobStatusResponse;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsPreviewRequest;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsPreviewResponse;
import com.mytutorplatform.vocabularyservice.model.dto.CreateWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordResponse;
import com.mytutorplatform.vocabularyservice.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @PostMapping("/batch/preview")
    public BatchWordsPreviewResponse previewBatch(@RequestBody @Valid BatchWordsPreviewRequest request) {
        return service.previewWordsBatch(request);
    }

    @PostMapping("/batch/create")
    public BatchWordsCreateResponse createBatch(@RequestBody @Valid BatchWordsCreateRequest request) {
        return service.createWordsBatch(request);
    }

    @PostMapping("/batch/jobs")
    public BatchWordsCreateJobResponse createBatchJob(@RequestBody @Valid BatchWordsCreateRequest request) {
        return service.createWordsBatchAsync(request);
    }

    @GetMapping("/batch/jobs/{jobId}")
    public BatchWordsCreateJobStatusResponse getBatchJobStatus(@PathVariable UUID jobId) {
        return service.getBatchJobStatus(jobId);
    }

    @GetMapping
    public Page<VocabularyWordResponse> getAll(
            @RequestParam(required = false) List<UUID> ids,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String difficulty,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.getAllWords(ids, text, difficulty, pageable);
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

    @PatchMapping("/{id}/audio/regenerate")
    public ResponseEntity<VocabularyWordResponse> regenerateAudio(@PathVariable UUID id,
                                                                  @RequestParam(required = false) AudioPart part) {
        return ResponseEntity.ok(service.regenerateAudio(id, part));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteWord(id);

        return ResponseEntity.noContent().build();
    }
}
