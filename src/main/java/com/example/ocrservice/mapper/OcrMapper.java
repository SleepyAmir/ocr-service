package com.example.ocrservice.mapper;

import com.example.ocrservice.document.OcrFile;
import com.example.ocrservice.document.OcrResult;
import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrDocumentSummaryResponse;
import com.example.ocrservice.dto.OcrFileResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * All OcrFile/OcrResult -> DTO conversion lives here. This class now does ONLY
 * mapping — the keyword-search/snippet logic that used to live in
 * buildSnippets(...) was moved to {@code TextSearchService}, because finding
 * matches and slicing context windows is search business logic, not mapping.
 *
 * Two of these methods take more than one source object (file + result).
 * MapStruct supports this directly: it resolves each target property from
 * whichever source parameter actually has a matching property name — no
 * ambiguity here since "extractedText" only exists on OcrResult and everything
 * else only exists on OcrFile.
 *
 * For the search response, the caller (OcrServiceImpl) now passes the already
 * computed {@code snippets} list in, so the Mapper just copies it onto the DTO
 * instead of computing it.
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
    @Mapping(target = "snippets", source = "snippets")
    public abstract OcrSearchResultResponse toSearchResponse(OcrFile file, List<String> snippets);

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
}
