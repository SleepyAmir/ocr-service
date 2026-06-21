package com.example.ocrservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Stores only the uploaded file itself and its metadata — no OCR result here.
 * Coordinated with the database team: this collection holds "the file",
 * OcrResult (separate collection) holds "the OCR text for that file".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ocr_files")
public class OcrFile {

    @Id
    private String id;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private Integer pageCount;

    /**
     * Like the professor's sample, we keep the uploaded file bytes in MongoDB.
     * For large production files, GridFS would be a better option.
     */
    private byte[] fileData;

    @Indexed
    private LocalDateTime createdAt;
}
