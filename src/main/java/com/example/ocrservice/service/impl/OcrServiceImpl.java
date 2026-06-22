package com.example.ocrservice.service.impl;

import com.example.ocrservice.document.OcrFile;
import com.example.ocrservice.document.OcrResult;
import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrDocumentSummaryResponse;
import com.example.ocrservice.dto.OcrFileResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import com.example.ocrservice.exception.ResourceNotFoundException;
import com.example.ocrservice.mapper.OcrMapper;
import com.example.ocrservice.repository.OcrFileRepository;
import com.example.ocrservice.repository.OcrResultRepository;
import com.example.ocrservice.service.OcrService;
import com.example.ocrservice.service.PersianTextNormalizer;
import com.example.ocrservice.service.TextSearchService;
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
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads/writes are split across two collections, coordinated with the
 * database team:
 *   - OcrFile   ("ocr_files")   -> file metadata + raw bytes
 *   - OcrResult ("ocr_results") -> extracted text, linked via fileId
 *
 * Each upload writes one OcrFile and one OcrResult (1:1, linked by fileId).
 * The public DTOs (OcrDocumentResponse, etc.) are unchanged, so existing
 * API consumers don't need to know about this internal split.
 *
 * DTO conversion is delegated to OcrMapper (MapStruct) — this class only
 * orchestrates OCR extraction and persistence, no manual "new XxxResponse(...)"
 * construction here anymore.
 */
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final OcrFileRepository fileRepository;
    private final OcrResultRepository resultRepository;
    private final OcrMapper ocrMapper;
    private final TextSearchService textSearchService;

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

        ExtractionResult extraction = extractText(bytes, contentType, file.getOriginalFilename());
        LocalDateTime now = LocalDateTime.now();

        OcrFile savedFile = fileRepository.save(OcrFile.builder()
                .originalFileName(file.getOriginalFilename())
                .contentType(normalizeContentType(contentType, file.getOriginalFilename()))
                .fileSize(file.getSize())
                .pageCount(extraction.pageCount())
                .fileData(bytes)
                .createdAt(now)
                .build());

        OcrResult savedResult = resultRepository.save(OcrResult.builder()
                .fileId(savedFile.getId())
                .extractedText(extraction.text())
                .normalizedText(PersianTextNormalizer.normalize(extraction.text()))
                .createdAt(now)
                .build());

        return ocrMapper.toDocumentResponse(savedFile, savedResult);
    }

    @Override
    public OcrDocumentResponse getDocument(String id) {
        OcrFile file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCR document not found with id: " + id));

        OcrResult result = resultRepository.findByFileId(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCR result not found for file id: " + id));

        return ocrMapper.toDocumentResponse(file, result);
    }

    @Override
    public OcrFileResponse getOriginalFile(String id) {
        OcrFile file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCR document not found with id: " + id));

        return ocrMapper.toFileResponse(file);
    }

    @Override
    public Page<OcrDocumentSummaryResponse> getRecentDocuments(Pageable pageable) {
        return fileRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ocrMapper::toSummaryResponse);
    }

    @Override
    public OcrSearchResultResponse searchInsideDocument(String id, String keyword) {
        validateKeyword(keyword);

        OcrFile file = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCR document not found with id: " + id));

        OcrResult result = resultRepository.findByFileId(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCR result not found for file id: " + id));

        List<String> snippets = textSearchService.buildSnippets(result.getExtractedText(), keyword);
        return ocrMapper.toSearchResponse(file, snippets);
    }

    @Override
    public Page<OcrSearchResultResponse> searchAllDocuments(String keyword, Pageable pageable) {
        validateKeyword(keyword);
        // Normalize the keyword the same way the stored text was normalized,
        // then regex-escape it so it's matched literally against normalizedText.
        String normalizedKeyword = PersianTextNormalizer.normalize(keyword).trim();
        if (normalizedKeyword.isEmpty()) {
            throw new IllegalArgumentException("keyword is required");
        }
        String escapedKeyword = Pattern.quote(normalizedKeyword);

        return resultRepository.searchByNormalizedTextRegex(escapedKeyword, pageable)
                .map(result -> {
                    OcrFile file = fileRepository.findById(result.getFileId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "OCR file not found with id: " + result.getFileId()));
                    List<String> snippets =
                            textSearchService.buildSnippets(result.getExtractedText(), keyword);
                    return ocrMapper.toSearchResponse(file, snippets);
                });
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
            return new File(tessdataPath).getAbsolutePath();
        }

        URL resource = getClass().getClassLoader().getResource("tessdata");
        if (resource == null) {
            throw new RuntimeException("tessdata was not found. Set OCR_TESSDATA_PATH or add tessdata to classpath.");
        }

        try {
            return new File(resource.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            return resource.getPath();
        }
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

    private record ExtractionResult(String text, int pageCount) {
    }
}