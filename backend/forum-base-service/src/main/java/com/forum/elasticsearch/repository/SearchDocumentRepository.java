package com.forum.elasticsearch.repository;

import com.forum.elasticsearch.document.SearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchDocumentRepository extends ElasticsearchRepository<SearchDocument, String> {
    List<SearchDocument> findByThreadId(Long threadId);
    void deleteByThreadId(Long threadId);
}
