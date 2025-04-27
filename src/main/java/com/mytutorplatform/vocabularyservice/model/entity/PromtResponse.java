package com.mytutorplatform.vocabularyservice.model.entity;

import lombok.Data;

@Data
public class PromtResponse {
     private String definition;
     private String[] synonyms;
     private String translationRu;
     private String partOfSpeech;
     private String phonetic;
     private String difficulty;
     private String popularity;
     private String exampleSentence;
}
