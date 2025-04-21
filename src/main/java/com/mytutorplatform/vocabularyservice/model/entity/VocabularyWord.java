package com.mytutorplatform.vocabularyservice.model.entity;


import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vocabulary_word",
        uniqueConstraints = @UniqueConstraint(columnNames = "text"))
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

    @Column(name = "definition_en", columnDefinition="TEXT")
    private String definitionEn;

    @Column(name = "synonyms_en", columnDefinition="TEXT[]")
    private String[] synonymsEn;

    private String phonetic;

    @Column(name = "audio_url")
    private String audioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "jsonb")
    private JsonNode rawJson;

    @Column(name = "created_by")
    private UUID createdByTeacherId;

    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}