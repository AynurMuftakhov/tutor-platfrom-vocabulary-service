package com.mytutorplatform.vocabularyservice;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class VocabularyPaginationAndSearchTest extends AbstractIntegrationTest {

    @Test
    void testPagination() throws Exception {
        UUID teacherId = UUID.randomUUID();
        createWord("apple_p", "яблоко", "noun", teacherId);
        createWord("banana_p", "банан", "noun", teacherId);
        createWord("cherry_p", "вишня", "noun", teacherId);

        mockMvc.perform(get("/api/v1/vocabulary/words")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void testSearchByText() throws Exception {
        UUID teacherId = UUID.randomUUID();
        createWord("apple_s", "яблоко", "noun", teacherId);
        createWord("application_s", "приложение", "noun", teacherId);
        createWord("banana_s", "банан", "noun", teacherId);

        mockMvc.perform(get("/api/v1/vocabulary/words")
                        .param("text", "app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].text").value("apple_s"))
                .andExpect(jsonPath("$.content[1].text").value("application_s"));
    }

    @Test
    void testSearchByTextCaseInsensitive() throws Exception {
        UUID teacherId = UUID.randomUUID();
        createWord("Apple", "яблоко", "noun", teacherId);

        mockMvc.perform(get("/api/v1/vocabulary/words")
                        .param("text", "app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].text").value("Apple"));
    }

    @Test
    void testSearchByDifficultyBeforePagination() throws Exception {
        UUID teacherId = UUID.randomUUID();
        for (int i = 0; i < 30; i++) {
            createWord("easy_" + i, "простой_" + i, "noun", "1", teacherId);
        }
        createWord("hard_one", "сложный_1", "noun", "5", teacherId);
        createWord("hard_two", "сложный_2", "noun", "5", teacherId);

        mockMvc.perform(get("/api/v1/vocabulary/words")
                        .param("difficulty", "5")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}
