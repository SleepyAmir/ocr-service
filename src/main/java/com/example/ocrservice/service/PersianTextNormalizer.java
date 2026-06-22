package com.example.ocrservice.service;

/**
 * Normalizes Persian/Arabic text so that search behaves the way a human
 * expects, regardless of which Unicode variant the OCR engine produced.
 *
 * Why this is needed
 * ------------------
 * The same Persian word can be stored with different code points:
 *   - Arabic Yeh "ي" (U+064A) vs Persian Yeh "ی" (U+06CC)
 *   - Arabic Kaf "ك" (U+0643) vs Persian Kaf "ک" (U+06A9)
 *   - Arabic-Indic digits "٠١٢" vs Persian "۰۱۲" vs ASCII "012"
 *   - ZERO WIDTH NON-JOINER (نیم‌فاصله, U+200C) and other invisible marks
 *   - Arabic diacritics / tatweel (ـ)
 * Tesseract often outputs the Arabic variants, while a user typing on a
 * Persian keyboard types the Persian ones — so a naive match misses them.
 *
 * Used in BOTH places so DB hits and snippet results stay consistent:
 *   - when saving (a normalized copy of the text is indexed and searched), and
 *   - when matching/snippeting in {@link TextSearchService}.
 *
 * This is intentionally safe for mixed Persian/English text: English letters,
 * digits and punctuation are left untouched (apart from ASCII-lowercasing,
 * which is what case-insensitive search needs anyway).
 */
public final class PersianTextNormalizer {

    private PersianTextNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            sb.append(mapChar(c));
        }

        // Lower-case so English parts of mixed text match case-insensitively.
        // (Persian letters have no case, so this only affects ASCII/Latin.)
        return sb.toString().toLowerCase();
    }

    private static char mapChar(char c) {
        switch (c) {
            // ----- Yeh variants -> Persian Yeh (ی) -----
            case '\u064A': // ARABIC YEH ي
            case '\u0649': // ARABIC ALEF MAKSURA ى
            case '\u06CC': // already Persian Yeh
                return '\u06CC';

            // ----- Kaf variants -> Persian Kaf (ک) -----
            case '\u0643': // ARABIC KAF ك
            case '\u06A9': // already Persian Kaf
                return '\u06A9';

            // ----- Heh variants -----
            case '\u0629': // ARABIC TEH MARBUTA ة -> Heh ه
                return '\u0647';

            // ----- Alef with hamza/madda -> bare Alef (ا) -----
            case '\u0622': // آ
            case '\u0623': // أ
            case '\u0625': // إ
                return '\u0627';

            // ----- Arabic-Indic digits -> ASCII -----
            case '\u0660': case '\u06F0': return '0';
            case '\u0661': case '\u06F1': return '1';
            case '\u0662': case '\u06F2': return '2';
            case '\u0663': case '\u06F3': return '3';
            case '\u0664': case '\u06F4': return '4';
            case '\u0665': case '\u06F5': return '5';
            case '\u0666': case '\u06F6': return '6';
            case '\u0667': case '\u06F7': return '7';
            case '\u0668': case '\u06F8': return '8';
            case '\u0669': case '\u06F9': return '9';

            // ----- Invisible / formatting marks -> normal space -----
            case '\u200C': // ZERO WIDTH NON-JOINER (نیم‌فاصله)
            case '\u200D': // ZERO WIDTH JOINER
            case '\u200E': // LEFT-TO-RIGHT MARK
            case '\u200F': // RIGHT-TO-LEFT MARK
            case '\u0640': // ARABIC TATWEEL ـ
                return ' ';

            default:
                // Strip Arabic diacritics (harakat): fatha, kasra, damma, etc.
                if (c >= '\u064B' && c <= '\u0652') {
                    return ' ';
                }
                return c;
        }
    }
}
