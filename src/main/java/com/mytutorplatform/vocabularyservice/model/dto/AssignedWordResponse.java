package com.mytutorplatform.vocabularyservice.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AssignedWordResponse {
    private UUID id;
    private UUID vocabularyWordId;
    private String text;
    private String translation;
    private String status;
    private int repetitionCount;
    private LocalDate lastCheckedDate;
}