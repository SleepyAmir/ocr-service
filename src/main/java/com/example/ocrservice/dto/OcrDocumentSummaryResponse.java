package com.example.ocrservice.dto;

import java.time.LocalDateTime;

public record OcrDocumentSummaryResponse(
        String id,
        String originalFileName,
        String contentType,
        Long fileSize,
        Integer pageCount,
        LocalDateTime createdAt
) {
}
