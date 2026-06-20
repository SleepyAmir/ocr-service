package com.example.ocrservice.controller;

import com.example.ocrservice.dto.OcrDocumentResponse;
import com.example.ocrservice.dto.OcrSearchResultResponse;
import com.example.ocrservice.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Plain Thymeleaf demo page for manual testing — separate from OcrController
 * (the REST API), which other services/clients still use as before.
 *
 * Flow:
 *   1. GET  /        -> upload form
 *   2. POST /extract  -> runs OCR, shows the extracted text below the form
 *   3. POST /search   -> keyword search across previously uploaded documents
 */
@Controller
@RequiredArgsConstructor
public class OcrWebController {

    private final OcrService ocrService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("result", null);
        model.addAttribute("searchResults", null);
        model.addAttribute("searchKeyword", "");
        return "index";
    }

    @PostMapping("/extract")
    public String extract(@RequestParam("file") MultipartFile file, Model model) {
        model.addAttribute("searchResults", null);
        model.addAttribute("searchKeyword", "");

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "لطفاً یک فایل انتخاب کنید.");
            model.addAttribute("result", null);
            return "index";
        }

        try {
            OcrDocumentResponse response = ocrService.extractAndSave(file);
            model.addAttribute("result", response);
            model.addAttribute("error", null);
        } catch (IOException | IllegalArgumentException e) {
            model.addAttribute("result", null);
            model.addAttribute("error", "خطا در پردازش فایل: " + e.getMessage());
        }

        return "index";
    }

    @PostMapping("/search")
    public String search(@RequestParam("keyword") String keyword, Model model) {
        model.addAttribute("result", null);
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