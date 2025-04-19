package com.mytutorplatform.vocabularyservice.model.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "vocabulary_word")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyWord {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private String translation;

    private String partOfSpeech;

    @Column(name = "created_by")
    private UUID createdByTeacherId;
}