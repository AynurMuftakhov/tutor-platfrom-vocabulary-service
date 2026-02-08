package com.mytutorplatform.vocabularyservice.model.dto;

public record BatchWordCreateFailure(
        String input,
        BatchFailureStage stage,
        String message
) {
}
