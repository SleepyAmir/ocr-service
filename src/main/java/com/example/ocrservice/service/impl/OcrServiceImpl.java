package com.example.ocrservice.service.impl;

import com.example.ocrservice.document.OcrDocument;
import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrDocumentSummaryResponse;
import com.example.ocrservice.dto.OcrFileResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import com.example.ocrservice.exception.ResourceNotFoundException;
import com.example.ocrservice.repository.OcrDocumentRepository;
import com.example.ocrservice.service.OcrService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final OcrDocumentRepository repository;

    @Value("${ocr.tessdata-path:}")
    private String tessdataPath;

    @Value("${ocr.language:fas+eng}")
    private String language;

    @Value("${ocr.pdf-dpi:250}")
    private int pdfDpi;

    @Override
    public OcrDocumentResponse extractAndSave(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded");
        }

        String contentType = file.getContentType();
        byte[] bytes = file.getBytes();

        ExtractionResult result = extractText(bytes, contentType, file.getOriginalFilename());

        OcrDocument saved = repository.save(OcrDocument.builder()
                .originalFileName(file.getOriginalFilename())
                .contentType(normalizeContentType(contentType, file.getOriginalFilename()))
                .fileSize(file.getSize())
                .pageCount(result.pageCount())
                .fileData(bytes)
                .extractedText(result.text())
                .createdAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    @Override
    public OcrDocumentResponse getDocument(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("OCR document not found with id: " + id));
    }

    @Override
    public OcrFileResponse getOriginalFile(String id) {
        OcrDocument document = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCR document not found with id: " + id));

        return new OcrFileResponse(
                safeFileName(document),
                document.getContentType(),
                document.getFileData()
        );
    }

    @Override
    public Page<OcrDocumentSummaryResponse> getRecentDocuments(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(this::toSummaryResponse);
    }

    @Override
    public OcrSearchResultResponse searchInsideDocument(String id, String keyword) {
        validateKeyword(keyword);

        OcrDocument document = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCR document not found with id: " + id));

        return toSearchResponse(document, keyword);
    }

    @Override
    public Page<OcrSearchResultResponse> searchAllDocuments(String keyword, Pageable pageable) {
        validateKeyword(keyword);
        String escapedKeyword = Pattern.quote(keyword.trim());
        return repository.searchByExtractedTextRegex(escapedKeyword, pageable)
                .map(document -> toSearchResponse(document, keyword));
    }

    private ExtractionResult extractText(byte[] bytes, String contentType, String fileName) {
        if (isPdf(contentType, fileName)) {
            return extractFromPdf(bytes);
        }
        if (isImage(contentType, fileName)) {
            return new ExtractionResult(extractFromImage(bytes), 1);
        }
        throw new IllegalArgumentException("Unsupported file type. Only image and PDF files are supported.");
    }

    private String extractFromImage(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                throw new IllegalArgumentException("Uploaded file is not a valid image");
            }
            return newTesseract().doOCR(image);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OCR failed for image: " + e.getMessage(), e);
        }
    }

    private ExtractionResult extractFromPdf(byte[] pdfData) {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(document);
            Tesseract tesseract = newTesseract();
            StringBuilder text = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, pdfDpi, ImageType.RGB);
                text.append("\n\n--- Page ").append(page + 1).append(" ---\n");
                text.append(tesseract.doOCR(image));
            }

            return new ExtractionResult(text.toString().trim(), document.getNumberOfPages());
        } catch (Exception e) {
            throw new RuntimeException("OCR failed for PDF: " + e.getMessage(), e);
        }
    }

    private Tesseract newTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(resolveTessdataPath());
        tesseract.setLanguage(language);
        return tesseract;
    }

    private String resolveTessdataPath() {
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            return tessdataPath;
        }

        URL resource = getClass().getClassLoader().getResource("tessdata");
        if (resource == null) {
            throw new RuntimeException("tessdata was not found. Set OCR_TESSDATA_PATH or add tessdata to classpath.");
        }
        return resource.getPath();
    }

    private boolean isPdf(String contentType, String fileName) {
        return "application/pdf".equalsIgnoreCase(contentType)
                || hasExtension(fileName, ".pdf");
    }

    private boolean isImage(String contentType, String fileName) {
        return (contentType != null && contentType.toLowerCase().startsWith("image/"))
                || hasAnyExtension(fileName, ".png", ".jpg", ".jpeg", ".bmp", ".tif", ".tiff", ".webp");
    }

    private String normalizeContentType(String contentType, String fileName) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        if (isPdf(null, fileName)) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private boolean hasAnyExtension(String fileName, String... extensions) {
        for (String extension : extensions) {
            if (hasExtension(fileName, extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExtension(String fileName, String extension) {
        return fileName != null && fileName.toLowerCase().endsWith(extension);
    }

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("keyword is required");
        }
    }

    private String safeFileName(OcrDocument document) {
        if (document.getOriginalFileName() != null && !document.getOriginalFileName().isBlank()) {
            return document.getOriginalFileName();
        }
        return "ocr-document-" + document.getId();
    }

    private OcrDocumentResponse toResponse(OcrDocument document) {
        return new OcrDocumentResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getPageCount(),
                document.getExtractedText(),
                document.getCreatedAt()
        );
    }

    private OcrDocumentSummaryResponse toSummaryResponse(OcrDocument document) {
        return new OcrDocumentSummaryResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getPageCount(),
                document.getCreatedAt()
        );
    }

    private OcrSearchResultResponse toSearchResponse(OcrDocument document, String keyword) {
        return new OcrSearchResultResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getPageCount(),
                buildSnippets(document.getExtractedText(), keyword),
                document.getCreatedAt()
        );
    }

    private List<String> buildSnippets(String text, String keyword) {
        List<String> snippets = new ArrayList<>();
        if (text == null || keyword == null || keyword.isBlank()) {
            return snippets;
        }

        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase().trim();
        int fromIndex = 0;

        while (snippets.size() < 5) {
            int index = lowerText.indexOf(lowerKeyword, fromIndex);
            if (index < 0) {
                break;
            }
            int start = Math.max(0, index - 60);
            int end = Math.min(text.length(), index + keyword.length() + 60);
            snippets.add(text.substring(start, end).replaceAll("\\s+", " ").trim());
            fromIndex = index + lowerKeyword.length();
        }

        return snippets;
    }

    private record ExtractionResult(String text, int pageCount) {
    }
}
