package com.mytutorplatform.vocabularyservice;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class VocabularyBatchControllerTest extends AbstractIntegrationTest {

    @Test
    void previewMarksCaseInsensitiveDuplicatesAndInvalidRows() throws Exception {
        UUID teacherId = UUID.randomUUID();
        createWord("Apple", "яблоко", "noun", teacherId);

        String body = """
                {
                  "teacherId": "%s",
                  "inputs": ["apple", "banana", "BANANA", "   ", "cherry"]
                }
                """.formatted(teacherId);

        mockMvc.perform(post("/api/v1/vocabulary/words/batch/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(5)))
                .andExpect(jsonPath("$.summary.total").value(5))
                .andExpect(jsonPath("$.summary.newCount").value(2))
                .andExpect(jsonPath("$.summary.duplicateCount").value(2))
                .andExpect(jsonPath("$.summary.invalidCount").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("DUPLICATE_REUSE"))
                .andExpect(jsonPath("$.rows[1].status").value("NEW"))
                .andExpect(jsonPath("$.rows[2].status").value("DUPLICATE_REUSE"))
                .andExpect(jsonPath("$.rows[3].status").value("INVALID"))
                .andExpect(jsonPath("$.rows[4].status").value("NEW"));
    }

    @Test
    void createReusesExistingCaseInsensitiveDuplicatesWithoutLlmCallWhenNoNewWords() throws Exception {
        UUID teacherId = UUID.randomUUID();
        UUID existingId = createWord("Apple", "яблоко", "noun", teacherId);

        String body = """
                {
                  "teacherId": "%s",
                  "inputs": ["apple", "APPLE"],
                  "reuseDuplicates": true,
                  "generateAudio": true
                }
                """.formatted(teacherId);

        mockMvc.perform(post("/api/v1/vocabulary/words/batch/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.total").value(2))
                .andExpect(jsonPath("$.summary.createdCount").value(0))
                .andExpect(jsonPath("$.summary.reusedCount").value(1))
                .andExpect(jsonPath("$.summary.failedCount").value(0))
                .andExpect(jsonPath("$.created", hasSize(0)))
                .andExpect(jsonPath("$.reused", hasSize(1)))
                .andExpect(jsonPath("$.reused[0].id").value(existingId.toString()))
                .andExpect(jsonPath("$.allWordIdsForHomework", hasSize(1)))
                .andExpect(jsonPath("$.allWordIdsForHomework[0]").value(existingId.toString()));
    }
}
