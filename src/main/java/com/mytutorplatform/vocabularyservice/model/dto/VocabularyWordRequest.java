package com.mytutorplatform.vocabularyservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class VocabularyWordRequest {
    @NotBlank
    private String text;

    @NotBlank
    private String translation;

    private String partOfSpeech;

    private UUID createdByTeacherId;
}
