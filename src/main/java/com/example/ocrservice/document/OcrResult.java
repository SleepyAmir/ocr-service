package com.example.ocrservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Stores the OCR result for one OcrFile — a separate collection from the
 * file itself, coordinated with the database team. One OcrFile has exactly
 * one OcrResult (1:1), linked through fileId (NOT a Mongo $lookup/join —
 * Spring Data MongoDB does a second query when you fetch by fileId).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ocr_results")
public class OcrResult {

    @Id
    private String id;

    /**
     * References OcrFile.id. Indexed (and effectively unique, enforced in
     * the service layer) since each file has exactly one OCR result.
     */
    @Indexed(unique = true)
    private String fileId;

    @TextIndexed
    private String extractedText;

    /**
     * A normalized copy of {@link #extractedText} (see PersianTextNormalizer):
     * Persian/Arabic letter variants unified, digits ASCII-fied, نیم‌فاصله and
     * diacritics removed, English lower-cased. DB keyword search runs against
     * THIS field so that database hits match the same way the Java snippet
     * logic does — keeping the two consistent.
     */
    private String normalizedText;

    @Indexed
    private LocalDateTime createdAt;
}
