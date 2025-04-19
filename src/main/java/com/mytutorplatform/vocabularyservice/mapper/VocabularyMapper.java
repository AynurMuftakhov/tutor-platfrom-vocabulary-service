package com.mytutorplatform.vocabularyservice.mapper;

import com.mytutorplatform.vocabularyservice.model.dto.AssignedWordResponse;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordRequest;
import com.mytutorplatform.vocabularyservice.model.dto.VocabularyWordResponse;
import com.mytutorplatform.vocabularyservice.model.entity.AssignedWord;
import com.mytutorplatform.vocabularyservice.model.entity.VocabularyWord;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VocabularyMapper {
    VocabularyWord toEntity(VocabularyWordRequest request);

    VocabularyWordResponse toResponse(VocabularyWord entity);

    List<VocabularyWordResponse> toListOfVocabularyWordResponses(List<VocabularyWord> entities);

    @Mapping(source = "vocabularyWord.id", target = "vocabularyWordId")
    @Mapping(source = "vocabularyWord.text", target = "text")
    @Mapping(source = "vocabularyWord.translation", target = "translation")
    AssignedWordResponse toResponse(AssignedWord assignedWord);

    List<AssignedWordResponse> toListOfAssignedWordResponses(List<AssignedWord> list);
}