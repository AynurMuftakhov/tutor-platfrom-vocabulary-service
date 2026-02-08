package com.mytutorplatform.vocabularyservice.model.dto;

import java.util.UUID;

public record BatchWordsCreateJobStatusResponse(
        UUID jobId,
        BatchJobStatus status,
        int totalCount,
        int processedCount,
        int createdCount,
        int reusedCount,
        int failedCount,
        int progressPct,
        String message,
        BatchWordsCreateResponse result
) {
}
