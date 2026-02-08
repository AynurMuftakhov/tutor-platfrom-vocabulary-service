package com.mytutorplatform.vocabularyservice.model.dto;

public record BatchWordsPreviewSummary(
        int total,
        int newCount,
        int duplicateCount,
        int invalidCount
) {
}
