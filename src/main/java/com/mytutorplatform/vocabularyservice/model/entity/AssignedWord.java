package com.mytutorplatform.vocabularyservice.model.entity;

import com.mytutorplatform.vocabularyservice.model.WordStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "assigned_word", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "vocabulary_word_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignedWord {

    @Id
    @UuidGenerator
    private UUID id;

    private UUID studentId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_word_id")
    private VocabularyWord vocabularyWord;

    @Column(nullable = false)
    private WordStatus status;

    @Column(nullable = false)
    private int repetitionCount = 0;

    private LocalDate lastCheckedDate;
}