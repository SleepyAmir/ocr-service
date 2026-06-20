package com.example.ocrservice.dto;

import java.time.LocalDateTime;

public record OcrDocumentResponse(
        String id,
        String originalFileName,
        String contentType,
        Long fileSize,
        Integer pageCount,
        String extractedText,
        LocalDateTime createdAt
) {
}
