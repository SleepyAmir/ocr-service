package com.example.ocrservice.repository;

import com.example.ocrservice.document.OcrResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OcrResultRepository extends MongoRepository<OcrResult, String> {

    Optional<OcrResult> findByFileId(String fileId);

    /**
     * Searches the NORMALIZED text (not the raw extractedText) so DB hits match
     * the same Persian/English-aware logic used by TextSearchService. The
     * caller must pass an already-normalized + regex-escaped keyword.
     */
    @Query("{ 'normalizedText': { $regex: ?0, $options: 'i' } }")
    Page<OcrResult> searchByNormalizedTextRegex(String keyword, Pageable pageable);
}