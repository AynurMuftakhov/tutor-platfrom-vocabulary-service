package com.mytutorplatform.vocabularyservice.model.dto;

import java.util.UUID;

public record BatchWordsCreateJobResponse(
        UUID jobId,
        BatchJobStatus status
) {
}
