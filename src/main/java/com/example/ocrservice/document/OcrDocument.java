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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ocr_documents")
public class OcrDocument {

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

    @TextIndexed
    private String extractedText;

    @Indexed
    private LocalDateTime createdAt;
}
