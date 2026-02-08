package com.mytutorplatform.vocabularyservice.service;

import com.mytutorplatform.vocabularyservice.llm.LLMService;
import com.mytutorplatform.vocabularyservice.mapper.VocabularyMapper;
import com.mytutorplatform.vocabularyservice.model.AudioPart;
import com.mytutorplatform.vocabularyservice.model.dto.BatchFailureStage;
import com.mytutorplatform.vocabularyservice.model.dto.BatchJobStatus;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordCreateFailure;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordPreviewRow;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordStatus;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateJobResponse;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateJobStatusResponse;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateRequest;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateResponse;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsCreateSummary;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsPreviewRequest;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsPreviewResponse;
import com.mytutorplatform.vocabularyservice.model.dto.BatchWordsPreviewSummary;
import com.mytutorplatform.vocabularyservice.model.dto.CreateWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordResponse;
import com.mytutorplatform.vocabularyservice.model.entity.PromtResponse;
import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import com.mytutorplatform.vocabularyservice.repository.VocabularyWordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyService {
    public static final String EXAMPLE_SENTENCE = "example_sentence_";
    private static final int MAX_BATCH_SIZE = 30;
    private static final int LLM_BATCH_CHUNK_SIZE = 10;

    private final VocabularyWordRepository wordRepo;
    private final VocabularyMapper mapper;
    private final LLMService lLMService;
    private final AudioStorageService audioStorageService;
    private final TextToSpeechService textToSpeechService;
    @Qualifier("vocabularyBatchJobExecutor")
    private final Executor vocabularyBatchJobExecutor;

    private final ConcurrentMap<UUID, BatchJobState> batchJobs = new ConcurrentHashMap<>();

    public VocabularyWordResponse saveWord(VocabularyWordRequest request) {
        VocabularyWord word = mapper.toEntity(request);

        return mapper.toResponse(wordRepo.save(word));
    }

    public VocabularyWordResponse createWord(CreateWordRequest request) {
        String text = sanitizeInput(request.getText());
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Word text is required");
        }

        Optional<VocabularyWord> existing = wordRepo.findByTextIgnoreCase(text);
        if (existing.isPresent()) {
            return mapper.toResponse(existing.get());
        }

        PromtResponse response = lLMService.callLlm(text);
        if (!isValidResponse(response)) {
            log.error("Invalid LLM response {}", response);
            throw new RuntimeException("Invalid LLM response");
        }

        VocabularyWord word = buildWordFromPrompt(text, request.getTeacherId(), response, true);
        return mapper.toResponse(wordRepo.save(word));
    }

    public BatchWordsPreviewResponse previewWordsBatch(BatchWordsPreviewRequest request) {
        validateBatchInputs(request.inputs());
        BatchEvaluation evaluation = evaluateBatch(request.inputs());
        return new BatchWordsPreviewResponse(
                evaluation.rows(),
                new BatchWordsPreviewSummary(
                        request.inputs().size(),
                        evaluation.newCount(),
                        evaluation.duplicateCount(),
                        evaluation.invalidCount()
                )
        );
    }

    public BatchWordsCreateResponse createWordsBatch(BatchWordsCreateRequest request) {
        validateBatchInputs(request.inputs());

        boolean reuseDuplicates = request.reuseDuplicates() == null || request.reuseDuplicates();
        boolean generateAudio = request.generateAudio() == null || request.generateAudio();
        BatchEvaluation evaluation = evaluateBatch(request.inputs());

        Map<UUID, VocabularyWordResponse> createdById = new LinkedHashMap<>();
        Map<UUID, VocabularyWordResponse> reusedById = new LinkedHashMap<>();
        List<BatchWordCreateFailure> failures = new ArrayList<>();
        Map<String, UUID> resolvedWordIdByNormalized = new HashMap<>();
        Map<UUID, VocabularyWordResponse> cachedExistingById = new HashMap<>();

        for (BatchWordPreviewRow row : evaluation.rows()) {
            if (row.status() != BatchWordStatus.DUPLICATE_REUSE || row.existingWordId() == null) {
                continue;
            }
            if (!reuseDuplicates) {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.VALIDATION, "duplicate_exists"));
                continue;
            }
            VocabularyWordResponse existing = getExistingWord(row.existingWordId(), cachedExistingById);
            if (existing == null) {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.DB, "existing_word_not_found"));
                continue;
            }
            reusedById.put(existing.getId(), existing);
            resolvedWordIdByNormalized.put(row.normalized(), existing.getId());
        }

        if (!reuseDuplicates) {
            for (BatchWordPreviewRow row : evaluation.rows()) {
                if (row.status() == BatchWordStatus.DUPLICATE_REUSE && row.existingWordId() == null) {
                    failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.VALIDATION, "duplicate_in_batch"));
                }
            }
        }

        for (BatchWordPreviewRow row : evaluation.rows()) {
            if (row.status() == BatchWordStatus.INVALID) {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.VALIDATION, row.reason()));
            }
        }

        List<BatchNewCandidate> candidates = evaluation.newCandidates();
        for (int start = 0; start < candidates.size(); start += LLM_BATCH_CHUNK_SIZE) {
            int end = Math.min(start + LLM_BATCH_CHUNK_SIZE, candidates.size());
            List<BatchNewCandidate> chunk = candidates.subList(start, end);
            List<String> chunkWords = chunk.stream().map(BatchNewCandidate::text).toList();
            List<PromtResponse> llmResponses = lLMService.callLlmBatch(chunkWords);

            for (int i = 0; i < chunk.size(); i++) {
                BatchNewCandidate candidate = chunk.get(i);
                PromtResponse response = i < llmResponses.size() ? llmResponses.get(i) : new PromtResponse();

                if (!isValidResponse(response)) {
                    response = lLMService.callLlm(candidate.text());
                }

                if (!isValidResponse(response)) {
                    failures.add(new BatchWordCreateFailure(candidate.text(), BatchFailureStage.LLM, "invalid_llm_response"));
                    continue;
                }

                Optional<VocabularyWord> existingWord = wordRepo.findByTextIgnoreCase(candidate.text());
                if (existingWord.isPresent()) {
                    if (!reuseDuplicates) {
                        failures.add(new BatchWordCreateFailure(candidate.text(), BatchFailureStage.VALIDATION, "duplicate_exists"));
                        continue;
                    }
                    VocabularyWordResponse existingResponse = mapper.toResponse(existingWord.get());
                    reusedById.put(existingResponse.getId(), existingResponse);
                    resolvedWordIdByNormalized.put(candidate.normalized(), existingResponse.getId());
                    continue;
                }

                try {
                    VocabularyWord entity = buildWordFromPrompt(candidate.text(), request.teacherId(), response, generateAudio);
                    VocabularyWord saved = wordRepo.save(entity);
                    VocabularyWordResponse createdResponse = mapper.toResponse(saved);
                    createdById.put(createdResponse.getId(), createdResponse);
                    resolvedWordIdByNormalized.put(candidate.normalized(), createdResponse.getId());
                } catch (Exception e) {
                    log.error("Failed to create word '{}'", candidate.text(), e);
                    failures.add(new BatchWordCreateFailure(candidate.text(), BatchFailureStage.DB, e.getMessage()));
                }
            }
        }

        LinkedHashSet<UUID> allWordIds = new LinkedHashSet<>();
        for (BatchWordPreviewRow row : evaluation.rows()) {
            if (row.status() == BatchWordStatus.INVALID) {
                continue;
            }
            if (!reuseDuplicates && row.status() == BatchWordStatus.DUPLICATE_REUSE) {
                continue;
            }
            UUID resolvedId = resolvedWordIdByNormalized.get(row.normalized());
            if (resolvedId != null) {
                allWordIds.add(resolvedId);
            } else {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.UNKNOWN, "word_not_created"));
            }
        }

        return new BatchWordsCreateResponse(
                new ArrayList<>(createdById.values()),
                new ArrayList<>(reusedById.values()),
                failures,
                new ArrayList<>(allWordIds),
                new BatchWordsCreateSummary(
                        request.inputs().size(),
                        createdById.size(),
                        reusedById.size(),
                        failures.size()
                )
        );
    }

    public BatchWordsCreateJobResponse createWordsBatchAsync(BatchWordsCreateRequest request) {
        validateBatchInputs(request.inputs());
        UUID jobId = UUID.randomUUID();
        BatchJobState state = new BatchJobState(jobId, request.inputs().size());
        batchJobs.put(jobId, state);

        vocabularyBatchJobExecutor.execute(() -> {
            state.markRunning();
            try {
                BatchWordsCreateResponse response = createWordsBatch(request, state);
                state.markCompleted(response);
            } catch (Exception e) {
                log.error("Batch job {} failed", jobId, e);
                state.markFailed(e.getMessage());
            }
        });

        return new BatchWordsCreateJobResponse(jobId, BatchJobStatus.QUEUED);
    }

    public BatchWordsCreateJobStatusResponse getBatchJobStatus(UUID jobId) {
        BatchJobState state = batchJobs.get(jobId);
        if (state == null) {
            throw new EntityNotFoundException("Batch job not found");
        }
        return state.toResponse();
    }

    private BatchWordsCreateResponse createWordsBatch(BatchWordsCreateRequest request, BatchJobState state) {
        validateBatchInputs(request.inputs());

        boolean reuseDuplicates = request.reuseDuplicates() == null || request.reuseDuplicates();
        boolean generateAudio = request.generateAudio() == null || request.generateAudio();
        BatchEvaluation evaluation = evaluateBatch(request.inputs());

        Map<UUID, VocabularyWordResponse> createdById = new LinkedHashMap<>();
        Map<UUID, VocabularyWordResponse> reusedById = new LinkedHashMap<>();
        List<BatchWordCreateFailure> failures = new ArrayList<>();
        Map<String, UUID> resolvedWordIdByNormalized = new HashMap<>();
        Map<UUID, VocabularyWordResponse> cachedExistingById = new HashMap<>();
        int processedCount = 0;

        for (BatchWordPreviewRow row : evaluation.rows()) {
            if (row.status() != BatchWordStatus.DUPLICATE_REUSE || row.existingWordId() == null) {
                continue;
            }
            if (!reuseDuplicates) {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.VALIDATION, "duplicate_exists"));
                processedCount++;
                state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Validating duplicates");
                continue;
            }
            VocabularyWordResponse existing = getExistingWord(row.existingWordId(), cachedExistingById);
            if (existing == null) {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.DB, "existing_word_not_found"));
                processedCount++;
                state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Resolving existing words");
                continue;
            }
            reusedById.put(existing.getId(), existing);
            resolvedWordIdByNormalized.put(row.normalized(), existing.getId());
            processedCount++;
            state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Reusing existing words");
        }

        if (!reuseDuplicates) {
            for (BatchWordPreviewRow row : evaluation.rows()) {
                if (row.status() == BatchWordStatus.DUPLICATE_REUSE && row.existingWordId() == null) {
                    failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.VALIDATION, "duplicate_in_batch"));
                    processedCount++;
                    state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Validating duplicates");
                }
            }
        }

        for (BatchWordPreviewRow row : evaluation.rows()) {
            if (row.status() == BatchWordStatus.INVALID) {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.VALIDATION, row.reason()));
                processedCount++;
                state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Validating inputs");
            }
        }

        List<BatchNewCandidate> candidates = evaluation.newCandidates();
        for (int start = 0; start < candidates.size(); start += LLM_BATCH_CHUNK_SIZE) {
            int end = Math.min(start + LLM_BATCH_CHUNK_SIZE, candidates.size());
            List<BatchNewCandidate> chunk = candidates.subList(start, end);
            List<String> chunkWords = chunk.stream().map(BatchNewCandidate::text).toList();
            List<PromtResponse> llmResponses = lLMService.callLlmBatch(chunkWords);

            for (int i = 0; i < chunk.size(); i++) {
                BatchNewCandidate candidate = chunk.get(i);
                PromtResponse response = i < llmResponses.size() ? llmResponses.get(i) : new PromtResponse();

                if (!isValidResponse(response)) {
                    response = lLMService.callLlm(candidate.text());
                }

                if (!isValidResponse(response)) {
                    failures.add(new BatchWordCreateFailure(candidate.text(), BatchFailureStage.LLM, "invalid_llm_response"));
                    processedCount++;
                    state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Generating word data");
                    continue;
                }

                Optional<VocabularyWord> existingWord = wordRepo.findByTextIgnoreCase(candidate.text());
                if (existingWord.isPresent()) {
                    if (!reuseDuplicates) {
                        failures.add(new BatchWordCreateFailure(candidate.text(), BatchFailureStage.VALIDATION, "duplicate_exists"));
                        processedCount++;
                        state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Checking duplicates");
                        continue;
                    }
                    VocabularyWordResponse existingResponse = mapper.toResponse(existingWord.get());
                    reusedById.put(existingResponse.getId(), existingResponse);
                    resolvedWordIdByNormalized.put(candidate.normalized(), existingResponse.getId());
                    processedCount++;
                    state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Reusing existing words");
                    continue;
                }

                try {
                    VocabularyWord entity = buildWordFromPrompt(candidate.text(), request.teacherId(), response, generateAudio);
                    VocabularyWord saved = wordRepo.save(entity);
                    VocabularyWordResponse createdResponse = mapper.toResponse(saved);
                    createdById.put(createdResponse.getId(), createdResponse);
                    resolvedWordIdByNormalized.put(candidate.normalized(), createdResponse.getId());
                    processedCount++;
                    state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Saving generated words");
                } catch (Exception e) {
                    log.error("Failed to create word '{}'", candidate.text(), e);
                    failures.add(new BatchWordCreateFailure(candidate.text(), BatchFailureStage.DB, e.getMessage()));
                    processedCount++;
                    state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Saving generated words");
                }
            }
        }

        LinkedHashSet<UUID> allWordIds = new LinkedHashSet<>();
        for (BatchWordPreviewRow row : evaluation.rows()) {
            if (row.status() == BatchWordStatus.INVALID) {
                continue;
            }
            if (!reuseDuplicates && row.status() == BatchWordStatus.DUPLICATE_REUSE) {
                continue;
            }
            UUID resolvedId = resolvedWordIdByNormalized.get(row.normalized());
            if (resolvedId != null) {
                allWordIds.add(resolvedId);
            } else {
                failures.add(new BatchWordCreateFailure(row.input(), BatchFailureStage.UNKNOWN, "word_not_created"));
                state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Finalizing");
            }
        }

        state.markProgress(processedCount, createdById.size(), reusedById.size(), failures.size(), "Finalizing");

        return new BatchWordsCreateResponse(
                new ArrayList<>(createdById.values()),
                new ArrayList<>(reusedById.values()),
                failures,
                new ArrayList<>(allWordIds),
                new BatchWordsCreateSummary(
                        request.inputs().size(),
                        createdById.size(),
                        reusedById.size(),
                        failures.size()
                )
        );
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

    private VocabularyWord buildWordFromPrompt(String text, UUID teacherId, PromtResponse response, boolean generateAudio) {
        String audioUrl = generateAudio ? generateAudio(text) : null;
        String exampleSentenceAudioUrl = generateAudio ? generateAudio(EXAMPLE_SENTENCE + text, response.getExampleSentence()) : null;

        return VocabularyWord.builder()
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
                .createdByTeacherId(teacherId)
                .editedAt(null)
                .build();
    }

    private VocabularyWordResponse getExistingWord(UUID id, Map<UUID, VocabularyWordResponse> cache) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        Optional<VocabularyWord> existing = wordRepo.findById(id);
        if (existing.isEmpty()) {
            cache.put(id, null);
            return null;
        }
        VocabularyWordResponse response = mapper.toResponse(existing.get());
        cache.put(id, response);
        return response;
    }

    private String sanitizeInput(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeForComparison(String sanitized) {
        return sanitized.toLowerCase(Locale.ROOT);
    }

    private void validateBatchInputs(List<String> inputs) {
        if (CollectionUtils.isEmpty(inputs)) {
            throw new IllegalArgumentException("Batch input cannot be empty");
        }
        if (inputs.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size cannot exceed " + MAX_BATCH_SIZE + " words");
        }
    }

    private BatchEvaluation evaluateBatch(List<String> inputs) {
        List<BatchWordPreviewRow> rows = new ArrayList<>();
        List<BatchNewCandidate> newCandidates = new ArrayList<>();
        Map<String, UUID> existingIdByNormalized = new HashMap<>();
        Set<String> seenNormalized = new HashSet<>();

        int newCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;

        for (String rawInput : inputs) {
            String sanitized = sanitizeInput(rawInput);
            if (!StringUtils.hasText(sanitized)) {
                rows.add(new BatchWordPreviewRow(rawInput, "", BatchWordStatus.INVALID, null, "blank"));
                invalidCount++;
                continue;
            }

            String normalized = normalizeForComparison(sanitized);
            boolean duplicateInBatch = seenNormalized.contains(normalized);
            seenNormalized.add(normalized);

            UUID existingId = existingIdByNormalized.get(normalized);
            if (existingId == null) {
                Optional<VocabularyWord> existingWord = wordRepo.findByTextIgnoreCase(sanitized);
                if (existingWord.isPresent()) {
                    existingId = existingWord.get().getId();
                    existingIdByNormalized.put(normalized, existingId);
                }
            }

            if (existingId != null) {
                rows.add(new BatchWordPreviewRow(rawInput, normalized, BatchWordStatus.DUPLICATE_REUSE, existingId, "already_exists"));
                duplicateCount++;
                continue;
            }

            if (duplicateInBatch) {
                rows.add(new BatchWordPreviewRow(rawInput, normalized, BatchWordStatus.DUPLICATE_REUSE, null, "duplicate_in_batch"));
                duplicateCount++;
                continue;
            }

            rows.add(new BatchWordPreviewRow(rawInput, normalized, BatchWordStatus.NEW, null, null));
            newCandidates.add(new BatchNewCandidate(sanitized, normalized));
            newCount++;
        }

        return new BatchEvaluation(rows, newCandidates, newCount, duplicateCount, invalidCount);
    }

    public Page<VocabularyWordResponse> getAllWords(List<UUID> ids, String text, String difficulty, Pageable pageable) {
        Specification<VocabularyWord> spec = Specification.where(null);

        if (!CollectionUtils.isEmpty(ids)) {
            spec = spec.and((root, query, cb) -> root.get("id").in(ids));
        }

        if (StringUtils.hasText(text)) {
            String normalizedText = text.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("text")), normalizedText));
        }

        if (StringUtils.hasText(difficulty)) {
            String normalizedDifficulty = difficulty.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("difficulty"), normalizedDifficulty));
        }

        return wordRepo.findAll(spec, pageable).map(mapper::toResponse);
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

    private record BatchEvaluation(
            List<BatchWordPreviewRow> rows,
            List<BatchNewCandidate> newCandidates,
            int newCount,
            int duplicateCount,
            int invalidCount
    ) {
    }

    private record BatchNewCandidate(String text, String normalized) {
    }

    private static final class BatchJobState {
        private final UUID jobId;
        private final int totalCount;
        private final OffsetDateTime createdAt;
        private volatile OffsetDateTime updatedAt;
        private volatile BatchJobStatus status;
        private volatile int processedCount;
        private volatile int createdCount;
        private volatile int reusedCount;
        private volatile int failedCount;
        private volatile String message;
        private volatile BatchWordsCreateResponse result;

        private BatchJobState(UUID jobId, int totalCount) {
            this.jobId = jobId;
            this.totalCount = totalCount;
            this.createdAt = OffsetDateTime.now();
            this.updatedAt = this.createdAt;
            this.status = BatchJobStatus.QUEUED;
            this.message = "Queued";
        }

        synchronized void markRunning() {
            this.status = BatchJobStatus.RUNNING;
            this.message = "Started";
            this.updatedAt = OffsetDateTime.now();
        }

        synchronized void markProgress(int processedCount, int createdCount, int reusedCount, int failedCount, String message) {
            this.createdCount = createdCount;
            this.reusedCount = reusedCount;
            this.failedCount = failedCount;
            this.processedCount = Math.min(totalCount, Math.max(0, processedCount));
            this.message = message;
            this.updatedAt = OffsetDateTime.now();
        }

        synchronized void markCompleted(BatchWordsCreateResponse result) {
            this.status = BatchJobStatus.COMPLETED;
            this.result = result;
            this.createdCount = result.summary().createdCount();
            this.reusedCount = result.summary().reusedCount();
            this.failedCount = result.summary().failedCount();
            this.processedCount = totalCount;
            this.message = "Completed";
            this.updatedAt = OffsetDateTime.now();
        }

        synchronized void markFailed(String reason) {
            this.status = BatchJobStatus.FAILED;
            this.message = reason == null ? "Failed" : reason;
            this.processedCount = Math.max(this.processedCount, Math.min(totalCount, createdCount + reusedCount + failedCount));
            this.updatedAt = OffsetDateTime.now();
        }

        BatchWordsCreateJobStatusResponse toResponse() {
            int progressPct = totalCount == 0 ? 100 : Math.min(100, (int) Math.round((processedCount * 100.0) / totalCount));
            return new BatchWordsCreateJobStatusResponse(
                    jobId,
                    status,
                    totalCount,
                    processedCount,
                    createdCount,
                    reusedCount,
                    failedCount,
                    progressPct,
                    message,
                    result
            );
        }
    }
}
