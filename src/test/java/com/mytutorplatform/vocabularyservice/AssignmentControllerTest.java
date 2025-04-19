package com.mytutorplatform.vocabularyservice;

import com.mytutorplatform.vocabularyservice.model.WordStatus;
import com.mytutorplatform.vocabularyservice.model.dto.AssignWordsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AssignmentControllerTest extends AbstractIntegrationTest {

    @Test
    public void testAssignWord_thenFindInList() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID firstWord = createWord(studentId.toString(), "english", "english", teacherId);
        UUID secondWord = createWord(studentId.toString(), "english2", "english2", teacherId);

        AssignWordsRequest assignWordsRequest = new AssignWordsRequest();
        assignWordsRequest.setStudentId(studentId);
        assignWordsRequest.setVocabularyWordIds(Arrays.asList(firstWord, secondWord));

        mockMvc.perform(post("/api/v1/vocabulary/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignWordsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].vocabularyWordId").value(firstWord.toString()))
                .andExpect(jsonPath("$[1].vocabularyWordId").value(secondWord.toString()));
    }

    @Test
    public void testGetAssignedWordsByStudentId() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID wordId = createWord(studentId.toString(), "getTest", "получить", teacherId);

        AssignWordsRequest request = new AssignWordsRequest();
        request.setStudentId(studentId);
        request.setVocabularyWordIds(List.of(wordId));

        mockMvc.perform(post("/api/v1/vocabulary/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vocabulary/students/" + studentId + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vocabularyWordId").value(wordId.toString()));
    }

    @Test
    public void testDeleteAssignmentById() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID wordId = createWord(studentId.toString(), "deleteTest", "удалить", teacherId);

        AssignWordsRequest request = new AssignWordsRequest();
        request.setStudentId(studentId);
        request.setVocabularyWordIds(List.of(wordId));

        String response = mockMvc.perform(post("/api/v1/vocabulary/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String assignmentId = objectMapper.readTree(response).get(0).get("id").asText();

        mockMvc.perform(patch("/api/v1/vocabulary/assignments/" + assignmentId + "/status")
                        .param("status", WordStatus.COMPLETED.name()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vocabulary/assignments/" + assignmentId))
                .andExpect(jsonPath("$.status").value(WordStatus.COMPLETED.name()));
    }
}
