package com.mytutorplatform.vocabularyservice.controller;

import com.mytutorplatform.vocabularyservice.constants.GenericConstants;
import com.mytutorplatform.vocabularyservice.model.WordStatus;
import com.mytutorplatform.vocabularyservice.model.dto.AssignWordsRequest;
import com.mytutorplatform.vocabularyservice.model.dto.AssignedWordResponse;
import com.mytutorplatform.vocabularyservice.service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(GenericConstants.API_V_1 + GenericConstants.VOCABULARY_PATH)
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService service;

    @PostMapping("/assignments")
    public List<AssignedWordResponse> assignWords(@RequestBody @Valid AssignWordsRequest request) {
        return service.assignWords(request);
    }

    @GetMapping("/students/{studentId}/assignments")
    public List<AssignedWordResponse> getStudentWords(@PathVariable UUID studentId,
                                                      @RequestParam(required = false) WordStatus status) {
        return service.getStudentWords(studentId, status);
    }

    @GetMapping("/assignments/{id}")
    public AssignedWordResponse getAssignmentById(@PathVariable UUID id) {
        return service.getAssignedWord(id);
    }

    @PatchMapping("/assignments/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestParam WordStatus status) {
        service.updateStatus(id, status);

        return ResponseEntity.ok().build();
    }
}
