package com.example.ocrservice.dto;

public record OcrFileResponse(
        String fileName,
        String contentType,
        byte[] data
) {
}
