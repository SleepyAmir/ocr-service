package com.example.ocrservice.service;

import java.util.List;

/**
 * Owns the keyword-search domain logic for OCR text.
 *
 * Previously this lived inside OcrMapper#buildSnippets, but building snippets
 * (finding matches, slicing context windows, limiting/cleaning the output) is
 * search business logic, not entity-to-DTO mapping. Keeping it here means:
 *   - the Mapper goes back to doing only mapping,
 *   - the search logic is independently unit-testable,
 *   - all "how search works" lives in one place next to the DB query in
 *     OcrServiceImpl/OcrResultRepository.
 */
public interface TextSearchService {

    /**
     * Returns true if {@code keyword} appears anywhere in {@code text}
     * (case-insensitive, trimmed). Mirrors the matching semantics used by
     * the MongoDB regex query so DB hits and snippet results stay consistent.
     */
    boolean matches(String text, String keyword);

    /**
     * Builds short, human-readable excerpts of {@code text} around each
     * occurrence of {@code keyword}, for display in search results.
     */
    List<String> buildSnippets(String text, String keyword);
}
