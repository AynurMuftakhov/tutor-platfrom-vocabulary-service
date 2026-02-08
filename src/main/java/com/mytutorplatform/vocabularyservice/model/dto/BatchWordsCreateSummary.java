package com.mytutorplatform.vocabularyservice.model.dto;

public record BatchWordsCreateSummary(
        int total,
        int createdCount,
        int reusedCount,
        int failedCount
) {
}
