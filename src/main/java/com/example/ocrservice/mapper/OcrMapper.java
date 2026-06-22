package com.example.ocrservice.mapper;

import com.example.ocrservice.document.OcrFile;
import com.example.ocrservice.document.OcrResult;
import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrDocumentSummaryResponse;
import com.example.ocrservice.dto.OcrFileResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * All OcrFile/OcrResult -> DTO conversion lives here, out of OcrServiceImpl,
 * which now only orchestrates OCR extraction + persistence.
 *
 * Two of these methods take more than one source object (file + result, or
 * file + result + keyword). MapStruct supports this directly: it resolves
 * each target property from whichever source parameter actually has a
 * matching property name, no ambiguity here since "extractedText" only
 * exists on OcrResult and everything else only exists on OcrFile.
 * "keyword" in toSearchResponse is a plain String (no bean properties), so
 * it's simply available inside the buildSnippets(...) expression below,
 * not auto-mapped to anything.
 *
 * Declared as an abstract class (not an interface) so the snippet-building
 * logic can live as a normal protected helper method right next to the
 * mapping method that uses it — same pattern as CourseMapper/CourseDetailMapper
 * in the platform project.
 */
@Mapper(componentModel = "spring")
public abstract class OcrMapper {

    @Mapping(target = "id", source = "file.id")
    @Mapping(target = "createdAt", source = "file.createdAt")
    @Mapping(target = "extractedText", source = "result.extractedText")
    public abstract OcrDocumentResponse toDocumentResponse(OcrFile file, OcrResult result);

    public abstract OcrDocumentSummaryResponse toSummaryResponse(OcrFile file);

    @Mapping(target = "fileName", expression = "java(resolveFileName(file))")
    @Mapping(target = "data", source = "file.fileData")
    public abstract OcrFileResponse toFileResponse(OcrFile file);

    @Mapping(target = "id", source = "file.id")
    @Mapping(target = "createdAt", source = "file.createdAt")
    @Mapping(target = "snippets", expression = "java(buildSnippets(result.getExtractedText(), keyword))")
    public abstract OcrSearchResultResponse toSearchResponse(OcrFile file, OcrResult result, String keyword);

    /**
     * Falls back to a generated name when the original filename is missing
     * or blank (e.g. some upload clients don't send one).
     */
    protected String resolveFileName(OcrFile file) {
        if (file.getOriginalFileName() != null && !file.getOriginalFileName().isBlank()) {
            return file.getOriginalFileName();
        }
        return "ocr-document-" + file.getId();
    }

    /**
     * Up to 5 short excerpts of extracted text around each keyword match,
     * whitespace-collapsed for clean display in search results.
     */
    protected List<String> buildSnippets(String text, String keyword) {
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
}