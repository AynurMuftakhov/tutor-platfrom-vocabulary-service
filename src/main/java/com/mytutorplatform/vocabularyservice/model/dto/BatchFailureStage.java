package com.mytutorplatform.vocabularyservice.model.dto;

public enum BatchFailureStage {
    LLM,
    AUDIO,
    VALIDATION,
    DB,
    UNKNOWN
}
