package com.mytutorplatform.vocabularyservice.repository;

import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, UUID> {
    Optional<VocabularyWord> findByTextIgnoreCase(String text);
}
