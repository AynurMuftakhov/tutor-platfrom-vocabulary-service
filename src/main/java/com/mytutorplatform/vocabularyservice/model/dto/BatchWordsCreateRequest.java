package com.mytutorplatform.vocabularyservice.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BatchWordsCreateRequest(
        @NotNull UUID teacherId,
        @NotEmpty List<String> inputs,
        Boolean reuseDuplicates,
        Boolean generateAudio
) {
}
