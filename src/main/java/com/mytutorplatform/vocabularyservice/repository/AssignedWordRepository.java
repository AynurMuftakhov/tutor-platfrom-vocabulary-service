package com.mytutorplatform.vocabularyservice.repository;

import com.mytutorplatform.vocabularyservice.model.WordStatus;
import com.mytutorplatform.vocabularyservice.model.entity.AssignedWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignedWordRepository extends JpaRepository<AssignedWord, UUID> {
    List<AssignedWord> findByStudentId(UUID studentId);

    List<AssignedWord> findByStudentIdAndStatus(UUID studentId, WordStatus status);
}
