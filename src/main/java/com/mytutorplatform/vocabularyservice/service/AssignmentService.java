package com.mytutorplatform.vocabularyservice.service;

import com.mytutorplatform.vocabularyservice.mapper.VocabularyMapper;
import com.mytutorplatform.vocabularyservice.model.WordStatus;
import com.mytutorplatform.vocabularyservice.model.dto.AssignWordsRequest;
import com.mytutorplatform.vocabularyservice.model.dto.AssignedWordResponse;
import com.mytutorplatform.vocabularyservice.model.entity.AssignedWord;
import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import com.mytutorplatform.vocabularyservice.repository.AssignedWordRepository;
import com.mytutorplatform.vocabularyservice.repository.VocabularyWordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final VocabularyWordRepository wordRepo;
    private final AssignedWordRepository assignedRepo;
    private final VocabularyMapper mapper;

    public List<AssignedWordResponse> assignWords(AssignWordsRequest request) {
        List<AssignedWord> assignments = request.getVocabularyWordIds().stream()
                .map(wordId -> {
                    VocabularyWord word = wordRepo.findById(wordId)
                            .orElseThrow(() -> new RuntimeException("Word not found: " + wordId));
                    return AssignedWord.builder()
                            .studentId(request.getStudentId())
                            .vocabularyWord(word)
                            .status(WordStatus.ASSIGNED)
                            .repetitionCount(0)
                            .build();
                }).toList();

        return mapper.toListOfAssignedWordResponses(assignedRepo.saveAll(assignments));
    }

    public AssignedWordResponse getAssignedWord(@PathVariable UUID assignmentId) {
        return mapper.toResponse(assignedRepo.findById(assignmentId).orElseThrow(() -> new EntityNotFoundException("Assignment not found")));
    }

    public List<AssignedWordResponse> getStudentWords(UUID studentId, WordStatus status) {
        List<AssignedWord> words = (status == null)
                ? assignedRepo.findByStudentId(studentId)
                : assignedRepo.findByStudentIdAndStatus(studentId, status);
        return mapper.toListOfAssignedWordResponses(words);
    }

    public void updateStatus(UUID assignmentId, WordStatus newStatus) {
        AssignedWord word = assignedRepo.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        word.setStatus(newStatus);
        assignedRepo.save(word);
    }
}
