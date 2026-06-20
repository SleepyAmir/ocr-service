package com.example.ocrservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OcrSearchResultResponse(
        String id,
        String originalFileName,
        Integer pageCount,
        List<String> snippets,
        LocalDateTime createdAt
) {
}
