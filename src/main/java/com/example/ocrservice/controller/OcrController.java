package com.example.ocrservice.controller;

import com.example.ocrservice.dto.ApiResponse;
import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrDocumentSummaryResponse;
import com.example.ocrservice.dto.OcrFileResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import com.example.ocrservice.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ocr")
public class OcrController {

    private final OcrService ocrService;

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("OCR service is running");
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OcrDocumentResponse> extractDocument(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok("OCR extraction completed", ocrService.extractAndSave(file));
    }

    @GetMapping("/documents")
    public ApiResponse<Page<OcrDocumentSummaryResponse>> getRecentDocuments(Pageable pageable) {
        return ApiResponse.ok(ocrService.getRecentDocuments(pageable));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<OcrDocumentResponse> getDocument(@PathVariable String id) {
        return ApiResponse.ok(ocrService.getDocument(id));
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<byte[]> downloadOriginalFile(@PathVariable String id) {
        OcrFileResponse file = ocrService.getOriginalFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.data());
    }

    @GetMapping("/documents/{id}/search")
    public ApiResponse<OcrSearchResultResponse> searchInsideDocument(
            @PathVariable String id,
            @RequestParam String keyword
    ) {
        return ApiResponse.ok(ocrService.searchInsideDocument(id, keyword));
    }

    @GetMapping("/search")
    public ApiResponse<Page<OcrSearchResultResponse>> searchAllDocuments(
            @RequestParam String keyword,
            Pageable pageable
    ) {
        return ApiResponse.ok(ocrService.searchAllDocuments(keyword, pageable));
    }
}
