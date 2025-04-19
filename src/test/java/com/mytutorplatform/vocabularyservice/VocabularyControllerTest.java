package com.mytutorplatform.vocabularyservice;

import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class VocabularyControllerTest extends AbstractIntegrationTest {
    private final UUID teacherId = UUID.randomUUID();

    @Test
    void testCreateWord_thenFindInList() throws Exception {
        UUID wordId = createWord("hello", "привет", "interjection", teacherId);

        mockMvc.perform(get("/api/v1/vocabulary/words"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(wordId.toString()))
                .andExpect(jsonPath("$[0].text").value("hello"));
    }

    @Test
    void testGetWordById() throws Exception {
        UUID wordId = createWord("apple", "яблоко", "noun", teacherId);

        mockMvc.perform(get("/api/v1/vocabulary/words/" + wordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("apple"))
                .andExpect(jsonPath("$.translation").value("яблоко"));
    }

    @Test
    void testUpdateWordTranslation() throws Exception {
        UUID wordId = createWord("apple", "яблоко", "noun", teacherId);

        VocabularyWordRequest updateRequest = new VocabularyWordRequest();
        updateRequest.setTranslation("яблоко_updated");

        mockMvc.perform(patch("/api/v1/vocabulary/words/" + wordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translation").value("яблоко_updated"));
    }

    @Test
    void testDeleteWord_thenGetNotFound() throws Exception {
        UUID wordId = createWord("apple", "яблоко", "noun", teacherId);

        mockMvc.perform(delete("/api/v1/vocabulary/words/" + wordId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/vocabulary/words/" + wordId))
                .andExpect(status().isNotFound());
    }
}
