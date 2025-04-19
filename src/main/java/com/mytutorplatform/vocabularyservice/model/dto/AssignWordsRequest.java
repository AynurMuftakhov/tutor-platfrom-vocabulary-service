package com.mytutorplatform.vocabularyservice.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignWordsRequest {
    @NotNull
    private UUID studentId;

    @NotNull
    private List<UUID> vocabularyWordIds;
}
