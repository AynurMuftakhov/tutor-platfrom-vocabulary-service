package com.mytutorplatform.vocabularyservice.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateWordRequest {
    private String text;
    private UUID teacherId;
}
