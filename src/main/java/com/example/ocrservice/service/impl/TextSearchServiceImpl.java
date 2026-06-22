package com.example.ocrservice.service.impl;

import com.example.ocrservice.service.PersianTextNormalizer;
import com.example.ocrservice.service.TextSearchService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link TextSearchService} implementation.
 *
 * Matching is done on a normalized copy of the text (see
 * {@link PersianTextNormalizer}) so that Persian/Arabic letter variants,
 * Persian vs ASCII digits, نیم‌فاصله, diacritics, and English letter case all
 * match the way a user expects — while the snippet itself is sliced from the
 * ORIGINAL text so the user still sees the real, untouched wording.
 *
 * This works because PersianTextNormalizer maps every character to exactly one
 * character (length-preserving), so an index found in the normalized string is
 * also a valid index into the original string.
 */
@Service
public class TextSearchServiceImpl implements TextSearchService {

    /** How many characters of context to show on each side of a match. */
    private static final int CONTEXT_RADIUS = 60;

    /** Maximum number of snippets returned per document. */
    private static final int MAX_SNIPPETS = 5;

    @Override
    public boolean matches(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return false;
        }
        String normalizedText = PersianTextNormalizer.normalize(text);
        String normalizedKeyword = PersianTextNormalizer.normalize(keyword).trim();
        if (normalizedKeyword.isEmpty()) {
            return false;
        }
        return normalizedText.contains(normalizedKeyword);
    }

    @Override
    public List<String> buildSnippets(String text, String keyword) {
        List<String> snippets = new ArrayList<>();
        if (text == null || keyword == null || keyword.isBlank()) {
            return snippets;
        }

        // Find matches on the normalized text...
        String normalizedText = PersianTextNormalizer.normalize(text);
        String normalizedKeyword = PersianTextNormalizer.normalize(keyword).trim();
        if (normalizedKeyword.isEmpty()) {
            return snippets;
        }

        int fromIndex = 0;
        while (snippets.size() < MAX_SNIPPETS) {
            int index = normalizedText.indexOf(normalizedKeyword, fromIndex);
            if (index < 0) {
                break;
            }
            // ...but slice the snippet from the ORIGINAL text (same indices,
            // because normalization is length-preserving) so the displayed
            // text keeps its real spelling/spacing.
            int start = Math.max(0, index - CONTEXT_RADIUS);
            int end = Math.min(text.length(), index + normalizedKeyword.length() + CONTEXT_RADIUS);
            snippets.add(text.substring(start, end).replaceAll("\\s+", " ").trim());
            fromIndex = index + normalizedKeyword.length();
        }

        return snippets;
    }
}
