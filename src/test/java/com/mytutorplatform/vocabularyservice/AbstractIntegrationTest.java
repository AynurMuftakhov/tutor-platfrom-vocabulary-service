package com.mytutorplatform.vocabularyservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AbstractIntegrationTest {
    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    protected MockMvc mockMvc;

    protected UUID createWord(String text, String translation, String partOfSpeech, UUID teacherId) throws Exception {
        VocabularyWordRequest request = new VocabularyWordRequest();
        request.setText(text);
        request.setTranslation(translation);
        request.setPartOfSpeech(partOfSpeech);
        request.setCreatedByTeacherId(teacherId);

        String response = mockMvc.perform(post("/api/v1/vocabulary/words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }
}
