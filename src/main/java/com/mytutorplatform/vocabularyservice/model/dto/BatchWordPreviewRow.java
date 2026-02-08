package com.mytutorplatform.vocabularyservice.model.dto;

import java.util.UUID;

public record BatchWordPreviewRow(
        String input,
        String normalized,
        BatchWordStatus status,
        UUID existingWordId,
        String reason
) {
}
