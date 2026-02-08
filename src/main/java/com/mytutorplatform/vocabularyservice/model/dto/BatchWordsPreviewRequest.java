package com.mytutorplatform.vocabularyservice.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BatchWordsPreviewRequest(
        @NotNull UUID teacherId,
        @NotEmpty List<String> inputs
) {
}
