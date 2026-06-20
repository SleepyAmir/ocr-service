package com.example.ocrservice.controller;

import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import com.example.ocrservice.service.OcrService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Plain Thymeleaf demo page for manual testing — separate from OcrController
 * (the REST API), which other services/clients still use as before.
 *
 * The last extracted document is kept in the HTTP session so it stays
 * visible on screen even after submitting the separate search form below it
 * (each form is its own full-page POST, so without the session the second
 * form's response would otherwise wipe out the first form's result).
 *
 * Flow:
 *   1. GET  /        -> upload form (shows last result/search from session, if any)
 *   2. POST /extract  -> runs OCR, stores the result in session, shows it
 *   3. POST /search   -> keyword search across previously uploaded documents,
 *                        reads the last OCR result back from session so it
 *                        still renders alongside the search results
 */
@Controller
@RequiredArgsConstructor
public class OcrWebController {

    private static final String SESSION_LAST_RESULT = "lastOcrResult";

    private final OcrService ocrService;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        model.addAttribute("result", session.getAttribute(SESSION_LAST_RESULT));
        model.addAttribute("searchResults", null);
        model.addAttribute("searchKeyword", "");
        return "index";
    }

    @PostMapping("/extract")
    public String extract(@RequestParam("file") MultipartFile file, HttpSession session, Model model) {
        model.addAttribute("searchResults", null);
        model.addAttribute("searchKeyword", "");

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "لطفاً یک فایل انتخاب کنید.");
            model.addAttribute("result", session.getAttribute(SESSION_LAST_RESULT));
            return "index";
        }

        try {
            OcrDocumentResponse response = ocrService.extractAndSave(file);
            session.setAttribute(SESSION_LAST_RESULT, response);
            model.addAttribute("result", response);
            model.addAttribute("error", null);
        } catch (IOException | IllegalArgumentException e) {
            model.addAttribute("result", session.getAttribute(SESSION_LAST_RESULT));
            model.addAttribute("error", "خطا در پردازش فایل: " + e.getMessage());
        }

        return "index";
    }

    @PostMapping("/search")
    public String search(@RequestParam("keyword") String keyword, HttpSession session, Model model) {
        // Keep showing the last extracted document, if any, alongside search results.
        model.addAttribute("result", session.getAttribute(SESSION_LAST_RESULT));
        model.addAttribute("searchKeyword", keyword);

        if (keyword == null || keyword.isBlank()) {
            model.addAttribute("error", "برای جست‌وجو یک کلمه وارد کنید.");
            model.addAttribute("searchResults", null);
            return "index";
        }

        try {
            Page<OcrSearchResultResponse> results =
                    ocrService.searchAllDocuments(keyword, PageRequest.of(0, 20));
            model.addAttribute("searchResults", results.getContent());
            model.addAttribute("error", null);
        } catch (IllegalArgumentException e) {
            model.addAttribute("searchResults", null);
            model.addAttribute("error", e.getMessage());
        }

        return "index";
    }
}