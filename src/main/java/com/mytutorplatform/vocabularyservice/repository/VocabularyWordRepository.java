package com.mytutorplatform.vocabularyservice.repository;

import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, UUID> {
    Optional<VocabularyWord> findByTextIgnoreCase(String text);
    Page<VocabularyWord> findByTextStartingWithIgnoreCase(String text, Pageable pageable);
    Page<VocabularyWord> findByIdIn(List<UUID> ids, Pageable pageable);
}
