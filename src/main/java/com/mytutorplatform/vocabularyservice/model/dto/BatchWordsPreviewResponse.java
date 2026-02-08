package com.mytutorplatform.vocabularyservice.model.dto;

import java.util.List;

public record BatchWordsPreviewResponse(
        List<BatchWordPreviewRow> rows,
        BatchWordsPreviewSummary summary
) {
}
