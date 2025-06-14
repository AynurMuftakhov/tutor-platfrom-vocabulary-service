package com.mytutorplatform.vocabularyservice.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class VocabularyWordResponse {
    private UUID id;
    private String text;
    private String translation;
    private String partOfSpeech;
    private UUID createdByTeacherId;
    private String definitionEn;
    private String[] synonymsEn;
    private String phonetic;
    private String audioUrl;
    private String exampleSentenceAudioUrl;
    private String difficulty;
    private String popularity;
    private String exampleSentence;
}
