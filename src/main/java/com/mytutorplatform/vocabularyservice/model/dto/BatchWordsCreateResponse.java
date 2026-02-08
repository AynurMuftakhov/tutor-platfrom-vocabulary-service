package com.mytutorplatform.vocabularyservice.model.dto;

import java.util.List;
import java.util.UUID;

public record BatchWordsCreateResponse(
        List<VocabularyWordResponse> created,
        List<VocabularyWordResponse> reused,
        List<BatchWordCreateFailure> failed,
        List<UUID> allWordIdsForHomework,
        BatchWordsCreateSummary summary
) {
}
