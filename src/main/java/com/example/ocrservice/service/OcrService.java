package com.example.ocrservice.service;

import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrDocumentSummaryResponse;
import com.example.ocrservice.dto.OcrFileResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface OcrService {

    OcrDocumentResponse extractAndSave(MultipartFile file) throws IOException;

    OcrDocumentResponse getDocument(String id);

    OcrFileResponse getOriginalFile(String id);

    Page<OcrDocumentSummaryResponse> getRecentDocuments(Pageable pageable);

    OcrSearchResultResponse searchInsideDocument(String id, String keyword);

    Page<OcrSearchResultResponse> searchAllDocuments(String keyword, Pageable pageable);
}
