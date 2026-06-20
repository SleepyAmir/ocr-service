package com.example.ocrservice.repository;

import com.example.ocrservice.document.OcrDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OcrDocumentRepository extends MongoRepository<OcrDocument, String> {

    Page<OcrDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("{ 'extractedText': { $regex: ?0, $options: 'i' } }")
    Page<OcrDocument> searchByExtractedTextRegex(String keyword, Pageable pageable);
}
