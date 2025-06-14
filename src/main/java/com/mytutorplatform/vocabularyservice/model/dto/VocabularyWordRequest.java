package com.mytutorplatform.vocabularyservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class VocabularyWordRequest {
    private String text;
    private String translation;
    private String partOfSpeech;
    private String definitionEn;
    private String[] synonymsEn;
    private String difficulty;
    private String popularity;
    private String exampleSentence;
}
