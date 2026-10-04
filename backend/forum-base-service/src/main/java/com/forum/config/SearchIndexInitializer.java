package com.forum.config;

import com.forum.elasticsearch.document.SearchDocument;
import com.forum.service.SearchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Component
public class SearchIndexInitializer implements CommandLineRunner {

    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchService searchService;

    public SearchIndexInitializer(ElasticsearchOperations elasticsearchOperations, SearchService searchService) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.searchService = searchService;
    }

    @Override
    public void run(String... args) {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(SearchDocument.class);
            if (!indexOps.exists()) {
                System.out.println(">>> Search index 'forum_search' not found. Creating it...");
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping(SearchDocument.class));
                System.out.println(">>> Search index 'forum_search' created and mapping applied successfully.");

                // Trigger automatic reindexing in a background thread to avoid blocking startup
                new Thread(() -> {
                    try {
                        System.out.println(">>> Starting automatic background reindexing...");
                        searchService.reindexAll();
                        System.out.println(">>> Background reindexing completed successfully.");
                    } catch (Exception ex) {
                        System.err.println(">>> Automatic background reindexing failed: " + ex.getMessage());
                    }
                }).start();
            } else {
                System.out.println(">>> Search index 'forum_search' already exists.");
            }
        } catch (Exception e) {
            System.err.println(">>> Failed to initialize search index 'forum_search': " + e.getMessage());
        }
    }
}
