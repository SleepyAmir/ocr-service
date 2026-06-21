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

    @Query("{ 'extractedText': { $regex: ?0, $options: 'i' } }")
    Page<OcrResult> searchByExtractedTextRegex(String keyword, Pageable pageable);
}